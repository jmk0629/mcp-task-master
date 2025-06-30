# API 문서화 및 테스트 자동화 가이드

## 개요

TofuMaker 백엔드 API의 문서화 및 테스트 자동화 시스템에 대한 종합적인 가이드입니다. 이 문서는 Swagger/OpenAPI 문서화, 단위 테스트, 통합 테스트, API 테스트 자동화, 그리고 테스트 데이터 관리에 대한 모든 정보를 포함합니다.

## 목차

1. [Swagger/OpenAPI 문서화](#swagger-openapi-문서화)
2. [단위 테스트](#단위-테스트)
3. [통합 테스트](#통합-테스트)
4. [API 테스트 자동화](#api-테스트-자동화)
5. [테스트 데이터 관리](#테스트-데이터-관리)
6. [유지보수 전략](#유지보수-전략)
7. [CI/CD 통합](#cicd-통합)
8. [모니터링 및 리포팅](#모니터링-및-리포팅)

## Swagger/OpenAPI 문서화

### 설정

#### 의존성
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-ui</artifactId>
    <version>1.7.0</version>
</dependency>

<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-security</artifactId>
    <version>1.7.0</version>
</dependency>
```

#### 설정 파일 (application.yml)
```yaml
springdoc:
  api-docs:
    path: /api-docs
    enabled: true
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    try-it-out-enabled: true
    operations-sorter: method
    tags-sorter: alpha
    display-request-duration: true
    doc-expansion: none
    default-models-expand-depth: 1
    default-model-expand-depth: 1
  show-actuator: true
  group-configs:
    - group: 'public'
      display-name: 'Public APIs'
      paths-to-match: 
        - /api/health/**
        - /api/boards/**
    - group: 'admin'
      display-name: 'Admin APIs'
      paths-to-match:
        - /api/cache/**
        - /api/database/**
```

### 접근 URL

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/api-docs`
- **Public APIs**: `http://localhost:8080/swagger-ui.html?configUrl=/api-docs/swagger-config#/public`
- **Admin APIs**: `http://localhost:8080/swagger-ui.html?configUrl=/api-docs/swagger-config#/admin`

### 어노테이션 사용법

#### 컨트롤러 레벨
```java
@RestController
@RequestMapping("/api/boards")
@Tag(name = "Board", description = "게시판 관리 API")
@SecurityRequirement(name = "Bearer Authentication") // 인증이 필요한 경우
public class BoardController {
    // ...
}
```

#### 메서드 레벨
```java
@Operation(summary = "게시글 생성", description = "새로운 게시글을 생성합니다.")
@ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "생성 성공",
                content = @Content(schema = @Schema(implementation = Board.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
})
@PostMapping
public ResponseEntity<Board> createBoard(
        @Parameter(description = "생성할 게시글 정보", required = true) @RequestBody Board board) {
    // ...
}
```

#### 엔티티 레벨
```java
@Entity
@Schema(description = "게시판 엔티티")
public class Board {
    
    @Id
    @Schema(description = "게시글 ID", example = "1")
    private Long id;
    
    @Schema(description = "게시글 제목", example = "안녕하세요", maxLength = 200)
    private String title;
    
    // ...
}
```

## 단위 테스트

### 테스트 구조

```
src/test/java/
├── com/tofumaker/
│   ├── controller/          # 컨트롤러 단위 테스트
│   │   ├── BoardControllerTest.java
│   │   └── HealthControllerTest.java
│   ├── service/             # 서비스 단위 테스트
│   │   ├── BoardServiceTest.java
│   │   └── CacheServiceTest.java
│   └── util/                # 유틸리티 클래스
│       └── TestDataManager.java
```

### 테스트 작성 가이드라인

#### 서비스 테스트 예시
```java
@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @InjectMocks
    private BoardService boardService;

    @Test
    void getBoardById_WhenBoardExists_ShouldReturnBoard() {
        // Given
        Long boardId = 1L;
        Board expectedBoard = new Board("제목", "내용", "작성자");
        when(boardRepository.findById(boardId)).thenReturn(Optional.of(expectedBoard));

        // When
        Board actualBoard = boardService.getBoardById(boardId);

        // Then
        assertNotNull(actualBoard);
        assertEquals(expectedBoard.getTitle(), actualBoard.getTitle());
        verify(boardRepository, times(1)).findById(boardId);
    }
}
```

#### 컨트롤러 테스트 예시
```java
@WebMvcTest(BoardController.class)
class BoardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BoardService boardService;

    @Test
    void createBoard_ShouldReturnCreatedBoard() throws Exception {
        // Given
        Board newBoard = new Board("새 제목", "새 내용", "새 작성자");
        when(boardService.createBoard(any(Board.class))).thenReturn(testBoard);

        // When & Then
        mockMvc.perform(post("/api/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newBoard)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("새 제목"));
    }
}
```

### 테스트 실행

```bash
# 모든 테스트 실행
mvn test

# 특정 테스트 클래스 실행
mvn test -Dtest=BoardServiceTest

# 특정 테스트 메서드 실행
mvn test -Dtest=BoardServiceTest#getBoardById_WhenBoardExists_ShouldReturnBoard

# 테스트 커버리지 리포트 생성
mvn jacoco:report
```

## 통합 테스트

### 설정

#### 테스트 프로파일 (application-test.yml)
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  redis:
    host: localhost
    port: 6379
    database: 1
```

### 통합 테스트 예시

```java
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@Transactional
class BoardIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private BoardRepository boardRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        boardRepository.deleteAll();
    }

    @Test
    void createAndRetrieveBoard_ShouldWorkEndToEnd() throws Exception {
        // 전체 플로우 테스트: 생성 -> 조회 -> 검증
    }
}
```

## API 테스트 자동화

### REST Assured 설정

#### 의존성
```xml
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>json-schema-validator</artifactId>
    <scope>test</scope>
</dependency>
```

### API 테스트 예시

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BoardApiTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    void createBoard_ShouldReturnCreatedBoard() {
        Board newBoard = new Board("API 테스트 제목", "API 테스트 내용", "API 테스트 작성자");

        given()
                .contentType(ContentType.JSON)
                .body(newBoard)
                .when()
                .post("/api/boards")
                .then()
                .statusCode(201)
                .body("title", equalTo("API 테스트 제목"))
                .body("id", notNullValue());
    }
}
```

### 테스트 시나리오

1. **CRUD 기본 기능 테스트**
   - 생성, 조회, 수정, 삭제 기능
   - 유효성 검증
   - 에러 처리

2. **검색 기능 테스트**
   - 제목 검색
   - 작성자 검색
   - 키워드 검색

3. **성능 테스트**
   - 응답 시간 검증
   - 동시 요청 처리

4. **보안 테스트**
   - 인증/인가 검증
   - 입력 데이터 검증

## 테스트 데이터 관리

### TestDataManager 사용법

```java
@Component
public class TestDataManager {
    
    @Autowired
    private BoardRepository boardRepository;
    
    private List<Long> createdBoardIds = new ArrayList<>();

    public Board createTestBoard(String title, String content, String author) {
        Board board = new Board(title, content, author);
        Board savedBoard = boardRepository.save(board);
        createdBoardIds.add(savedBoard.getId());
        return savedBoard;
    }

    public void cleanupTestData() {
        if (!createdBoardIds.isEmpty()) {
            boardRepository.deleteAllById(createdBoardIds);
            createdBoardIds.clear();
        }
    }
}
```

### 테스트 데이터 생성 전략

1. **기본 테스트 데이터**
   - 각 테스트마다 필요한 최소한의 데이터만 생성
   - 테스트 간 독립성 보장

2. **검색 테스트 데이터**
   - 다양한 검색 시나리오를 위한 데이터 세트
   - 키워드, 작성자, 날짜별 데이터

3. **성능 테스트 데이터**
   - 대량 데이터 생성 및 관리
   - 메모리 효율적인 데이터 생성

### 데이터 정리 전략

```java
@AfterEach
void tearDown() {
    testDataManager.cleanupTestData();
}

@AfterAll
static void tearDownAll() {
    testDataManager.cleanupAllBoardData();
}
```

## 유지보수 전략

### 1. 자동화된 문서 업데이트

#### OpenAPI 스펙 검증
```java
@Test
void openApiSpec_ShouldBeValid() {
    given()
            .when()
            .get("/api-docs")
            .then()
            .statusCode(200)
            .body("openapi", equalTo("3.0.1"))
            .body("info.title", notNullValue())
            .body("paths", notNullValue());
}
```

#### 스키마 검증
```java
@Test
void boardResponse_ShouldMatchSchema() {
    given()
            .when()
            .get("/api/boards/1")
            .then()
            .statusCode(200)
            .body(matchesJsonSchemaInClasspath("schemas/board-schema.json"));
}
```

### 2. 테스트 자동화 파이프라인

#### GitHub Actions 예시
```yaml
name: API Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 11
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'temurin'
    
    - name: Run tests
      run: mvn clean test
    
    - name: Generate test report
      run: mvn jacoco:report
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
```

### 3. 문서 동기화 체크

#### API 변경 감지
```java
@Test
void apiEndpoints_ShouldMatchDocumentation() {
    // 실제 엔드포인트와 문서화된 엔드포인트 비교
    Set<String> documentedEndpoints = getDocumentedEndpoints();
    Set<String> actualEndpoints = getActualEndpoints();
    
    assertEquals(documentedEndpoints, actualEndpoints, 
        "API 엔드포인트가 문서와 일치하지 않습니다.");
}
```

### 4. 버전 관리 전략

#### API 버전 관리
```java
@RestController
@RequestMapping("/api/v1/boards")
@Tag(name = "Board API v1", description = "게시판 API 버전 1")
public class BoardControllerV1 {
    // v1 구현
}

@RestController
@RequestMapping("/api/v2/boards")
@Tag(name = "Board API v2", description = "게시판 API 버전 2")
public class BoardControllerV2 {
    // v2 구현
}
```

#### 하위 호환성 테스트
```java
@Test
void apiV1_ShouldMaintainBackwardCompatibility() {
    // v1 API의 하위 호환성 검증
}
```

## CI/CD 통합

### 1. 테스트 단계별 실행

```bash
# 1단계: 단위 테스트
mvn test -Dtest="**/*Test.java"

# 2단계: 통합 테스트
mvn test -Dtest="**/*IntegrationTest.java"

# 3단계: API 테스트
mvn test -Dtest="**/*ApiTest.java"

# 4단계: 성능 테스트
mvn test -Dtest="**/*PerformanceTest.java"
```

### 2. 품질 게이트

```yaml
quality_gates:
  test_coverage: 80%
  api_response_time: 1000ms
  documentation_coverage: 90%
```

### 3. 자동 배포 조건

- 모든 테스트 통과
- 코드 커버리지 80% 이상
- API 문서 동기화 확인
- 성능 기준 충족

## 모니터링 및 리포팅

### 1. 테스트 메트릭

- **테스트 커버리지**: JaCoCo 리포트
- **API 응답 시간**: REST Assured 성능 테스트
- **문서 동기화 상태**: 자동화된 검증

### 2. 대시보드

- **SonarQube**: 코드 품질 및 커버리지
- **Swagger UI**: API 문서 및 테스트
- **Grafana**: 성능 모니터링

### 3. 알림 설정

- 테스트 실패 시 Slack 알림
- 커버리지 감소 시 이메일 알림
- API 문서 불일치 시 PR 코멘트

## 트러블슈팅

### 일반적인 문제들

1. **테스트 데이터 충돌**
   - 해결: `@Transactional` 사용 또는 테스트 후 데이터 정리

2. **포트 충돌**
   - 해결: `@SpringBootTest(webEnvironment = RANDOM_PORT)` 사용

3. **캐시 간섭**
   - 해결: 테스트 전 캐시 클리어

4. **비동기 처리 테스트**
   - 해결: `@Async` 테스트를 위한 적절한 대기 시간 설정

### 성능 최적화

1. **테스트 실행 시간 단축**
   - 병렬 테스트 실행
   - 테스트 데이터 최소화
   - 불필요한 Spring Context 로딩 방지

2. **메모리 사용량 최적화**
   - 테스트 후 리소스 정리
   - 대용량 테스트 데이터 스트리밍

## 결론

이 가이드를 통해 TofuMaker 백엔드 API의 문서화와 테스트 자동화 시스템을 효과적으로 관리할 수 있습니다. 지속적인 개선과 모니터링을 통해 API의 품질과 안정성을 보장하세요.

### 다음 단계

1. 추가 API 엔드포인트에 대한 문서화 및 테스트 작성
2. 성능 테스트 시나리오 확장
3. 보안 테스트 강화
4. 모니터링 대시보드 구축
5. 자동화된 리포팅 시스템 구현 