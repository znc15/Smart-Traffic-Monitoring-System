// 将后端返回的详情信息翻译为中文
export const translateBackendDetailToZh = (detail: string): string => {
  const map: Record<string, string> = {
    "Not authenticated": "未登录",
    Forbidden: "无权限",
    "Internal server error": "服务器内部错误",
  };

  return map[detail] ?? detail;
};
