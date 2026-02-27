import type { GlobalThemeOverrides } from 'naive-ui'

// Design tokens - 清爽明亮商务风
const colors = {
  primary: '#2080f0',
  primaryHover: '#4098fc',
  primaryPressed: '#1060c9',
  primarySuppl: '#4098fc',

  success: '#18a058',
  successHover: '#36ad6a',
  successPressed: '#0c7a43',

  warning: '#f0a020',
  warningHover: '#fcb040',
  warningPressed: '#c97c10',

  error: '#d03050',
  errorHover: '#de576d',
  errorPressed: '#ab1f3f',

  info: '#2080f0',
  infoHover: '#4098fc',
  infoPressed: '#1060c9',
}

const neutral = {
  textBase: '#1f2937',
  textSecondary: '#6b7280',
  textTertiary: '#9ca3af',
  border: '#e5e7eb',
  divider: '#f0f0f0',
  background: '#f8fafc',
  cardBackground: '#ffffff',
  tableHeader: '#fafafa',
}

const radius = {
  small: '4px',
  medium: '6px',
  large: '8px',
}

export const themeOverrides: GlobalThemeOverrides = {
  common: {
    primaryColor: colors.primary,
    primaryColorHover: colors.primaryHover,
    primaryColorPressed: colors.primaryPressed,
    primaryColorSuppl: colors.primarySuppl,

    successColor: colors.success,
    successColorHover: colors.successHover,
    successColorPressed: colors.successPressed,

    warningColor: colors.warning,
    warningColorHover: colors.warningHover,
    warningColorPressed: colors.warningPressed,

    errorColor: colors.error,
    errorColorHover: colors.errorHover,
    errorColorPressed: colors.errorPressed,

    infoColor: colors.info,
    infoColorHover: colors.infoHover,
    infoColorPressed: colors.infoPressed,

    textColorBase: neutral.textBase,
    textColor1: neutral.textBase,
    textColor2: neutral.textSecondary,
    textColor3: neutral.textTertiary,

    dividerColor: neutral.divider,
    borderColor: neutral.border,

    borderRadius: radius.medium,
    borderRadiusSmall: radius.small,

    fontFamily:
      '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans", sans-serif',
    fontSize: '14px',
    fontSizeMedium: '14px',
    fontSizeLarge: '16px',
    fontSizeHuge: '18px',
  },
  Button: {
    borderRadiusMedium: radius.medium,
    borderRadiusLarge: radius.large,
    heightMedium: '36px',
    heightLarge: '40px',
  },
  Card: {
    borderRadius: radius.large,
    color: neutral.cardBackground,
    borderColor: neutral.border,
  },
  DataTable: {
    thColor: neutral.tableHeader,
    borderColor: neutral.border,
    borderRadius: radius.large,
  },
  Input: {
    borderRadius: radius.medium,
    heightMedium: '36px',
  },
  Tag: {
    borderRadius: radius.small,
  },
  Menu: {
    borderRadius: radius.medium,
  },
}

export { colors, neutral, radius }
