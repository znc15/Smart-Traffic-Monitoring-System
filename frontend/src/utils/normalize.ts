type AnyRecord = Record<string, unknown>;

const asRecord = (value: unknown): AnyRecord =>
  value && typeof value === "object" ? (value as AnyRecord) : {};

const pick = (record: AnyRecord, ...keys: string[]): unknown => {
  for (const key of keys) {
    if (key in record) {
      return record[key];
    }
  }
  return undefined;
};

const toStringValue = (value: unknown, fallback = ""): string => {
  if (typeof value === "string") {
    return value;
  }
  if (typeof value === "number" || typeof value === "boolean") {
    return String(value);
  }
  return fallback;
};

const toNullableString = (value: unknown): string | null => {
  if (typeof value === "string" && value.trim() !== "") {
    return value;
  }
  return null;
};

const toNumberValue = (value: unknown, fallback = 0): number => {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === "string" && value.trim() !== "") {
    const parsed = Number(value);
    if (Number.isFinite(parsed)) {
      return parsed;
    }
  }
  return fallback;
};

const toBooleanValue = (value: unknown, fallback = false): boolean => {
  if (typeof value === "boolean") {
    return value;
  }
  if (typeof value === "string") {
    if (value === "true" || value === "1") return true;
    if (value === "false" || value === "0") return false;
  }
  return fallback;
};

export type NormalizedCamera = {
  id: number;
  name: string;
  location: string;
  stream_url: string | null;
  road_name: string | null;
  enabled: boolean;
};

export const normalizeCamera = (raw: unknown): NormalizedCamera => {
  const record = asRecord(raw);
  return {
    id: toNumberValue(pick(record, "id"), 0),
    name: toStringValue(pick(record, "name"), ""),
    location: toStringValue(pick(record, "location"), ""),
    stream_url: toNullableString(pick(record, "stream_url", "streamUrl")),
    road_name: toNullableString(pick(record, "road_name", "roadName")),
    enabled: toBooleanValue(pick(record, "enabled"), true),
  };
};

export type NormalizedAdminUser = {
  id: number;
  username: string;
  email: string;
  phone_number: string;
  role_id: number;
  enabled: boolean;
  created_at: string;
};

export const normalizeAdminUser = (raw: unknown): NormalizedAdminUser => {
  const record = asRecord(raw);
  return {
    id: toNumberValue(pick(record, "id"), 0),
    username: toStringValue(pick(record, "username"), ""),
    email: toStringValue(pick(record, "email"), ""),
    phone_number: toStringValue(
      pick(record, "phone_number", "phoneNumber"),
      ""
    ),
    role_id: toNumberValue(pick(record, "role_id", "roleId"), 1),
    enabled: toBooleanValue(pick(record, "enabled"), true),
    created_at: toStringValue(pick(record, "created_at", "createdAt"), ""),
  };
};

export type NormalizedSiteSettings = {
  site_name: string;
  logo_url: string;
  announcement: string;
  footer_text: string;
};

export const normalizeSiteSettings = (raw: unknown): NormalizedSiteSettings => {
  const record = asRecord(raw);
  return {
    site_name: toStringValue(pick(record, "site_name", "siteName"), ""),
    logo_url: toStringValue(pick(record, "logo_url", "logoUrl"), ""),
    announcement: toStringValue(pick(record, "announcement"), ""),
    footer_text: toStringValue(pick(record, "footer_text", "footerText"), ""),
  };
};

export const normalizeDensityStatus = (
  value: unknown
): "clear" | "busy" | "congested" | "offline" | undefined => {
  const raw = toStringValue(value, "").toLowerCase();
  if (!raw) return undefined;
  if (raw === "congested" || raw === "拥堵" || raw === "high") return "congested";
  if (raw === "busy" || raw === "较拥挤") return "busy";
  if (raw === "clear" || raw === "畅通" || raw === "normal") return "clear";
  if (raw === "offline" || raw === "离线") return "offline";
  return undefined;
};

export const normalizeSpeedStatus = (
  value: unknown
): "slow" | "fast" | "unknown" | undefined => {
  const raw = toStringValue(value, "").toLowerCase();
  if (!raw) return undefined;
  if (raw === "fast" || raw === "较快") return "fast";
  if (raw === "slow" || raw === "较慢") return "slow";
  if (raw === "unknown" || raw === "未知") return "unknown";
  return undefined;
};
