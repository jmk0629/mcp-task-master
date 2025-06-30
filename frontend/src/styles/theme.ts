import { Theme } from '../types';

export const theme: Theme = {
  colors: {
    primary: '#2563eb',      // Blue-600
    secondary: '#64748b',    // Slate-500
    success: '#059669',      // Emerald-600
    warning: '#d97706',      // Amber-600
    error: '#dc2626',        // Red-600
    background: '#f8fafc',   // Slate-50
    surface: '#ffffff',      // White
    text: {
      primary: '#0f172a',    // Slate-900
      secondary: '#64748b',  // Slate-500
    },
  },
  spacing: {
    xs: '0.25rem',   // 4px
    sm: '0.5rem',    // 8px
    md: '1rem',      // 16px
    lg: '1.5rem',    // 24px
    xl: '2rem',      // 32px
  },
  borderRadius: {
    sm: '0.25rem',   // 4px
    md: '0.5rem',    // 8px
    lg: '0.75rem',   // 12px
  },
};

// CSS 변수로 테마 적용
export const applyTheme = () => {
  const root = document.documentElement;
  
  // Colors
  root.style.setProperty('--color-primary', theme.colors.primary);
  root.style.setProperty('--color-secondary', theme.colors.secondary);
  root.style.setProperty('--color-success', theme.colors.success);
  root.style.setProperty('--color-warning', theme.colors.warning);
  root.style.setProperty('--color-error', theme.colors.error);
  root.style.setProperty('--color-background', theme.colors.background);
  root.style.setProperty('--color-surface', theme.colors.surface);
  root.style.setProperty('--color-text-primary', theme.colors.text.primary);
  root.style.setProperty('--color-text-secondary', theme.colors.text.secondary);
  
  // Spacing
  root.style.setProperty('--spacing-xs', theme.spacing.xs);
  root.style.setProperty('--spacing-sm', theme.spacing.sm);
  root.style.setProperty('--spacing-md', theme.spacing.md);
  root.style.setProperty('--spacing-lg', theme.spacing.lg);
  root.style.setProperty('--spacing-xl', theme.spacing.xl);
  
  // Border Radius
  root.style.setProperty('--border-radius-sm', theme.borderRadius.sm);
  root.style.setProperty('--border-radius-md', theme.borderRadius.md);
  root.style.setProperty('--border-radius-lg', theme.borderRadius.lg);
}; 