import { create } from 'zustand';
import { devtools } from 'zustand/middleware';

interface AppState {
  // UI 상태
  sidebarOpen: boolean;
  theme: 'light' | 'dark';
  loading: boolean;
  
  // 사용자 상태
  user: {
    id?: string;
    name?: string;
    email?: string;
  } | null;
  
  // 알림 상태
  notifications: Array<{
    id: string;
    type: 'success' | 'error' | 'warning' | 'info';
    message: string;
    timestamp: number;
  }>;
}

interface AppActions {
  // UI 액션
  toggleSidebar: () => void;
  setSidebarOpen: (open: boolean) => void;
  setTheme: (theme: 'light' | 'dark') => void;
  setLoading: (loading: boolean) => void;
  
  // 사용자 액션
  setUser: (user: AppState['user']) => void;
  clearUser: () => void;
  
  // 알림 액션
  addNotification: (notification: Omit<AppState['notifications'][0], 'id' | 'timestamp'>) => void;
  removeNotification: (id: string) => void;
  clearNotifications: () => void;
}

type AppStore = AppState & AppActions;

export const useAppStore = create<AppStore>()(
  devtools(
    (set, get) => ({
      // 초기 상태
      sidebarOpen: false,
      theme: 'light',
      loading: false,
      user: null,
      notifications: [],
      
      // UI 액션
      toggleSidebar: () => set((state) => ({ sidebarOpen: !state.sidebarOpen })),
      setSidebarOpen: (open) => set({ sidebarOpen: open }),
      setTheme: (theme) => set({ theme }),
      setLoading: (loading) => set({ loading }),
      
      // 사용자 액션
      setUser: (user) => set({ user }),
      clearUser: () => set({ user: null }),
      
      // 알림 액션
      addNotification: (notification) => {
        const id = Math.random().toString(36).substr(2, 9);
        const timestamp = Date.now();
        set((state) => ({
          notifications: [...state.notifications, { ...notification, id, timestamp }]
        }));
        
        // 5초 후 자동 제거
        setTimeout(() => {
          get().removeNotification(id);
        }, 5000);
      },
      removeNotification: (id) => set((state) => ({
        notifications: state.notifications.filter(n => n.id !== id)
      })),
      clearNotifications: () => set({ notifications: [] }),
    }),
    {
      name: 'app-store',
    }
  )
); 