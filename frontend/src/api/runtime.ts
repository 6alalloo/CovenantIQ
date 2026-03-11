import type { RuntimeConfig } from "../types/api";
import { requestPublic } from "./core/http";

export function getRuntimeConfig() {
  return requestPublic<RuntimeConfig>("/runtime-config");
}
