import createClient from "openapi-fetch";
import type { paths } from "./schema";

// اگر کنترلرهای بک‌اند پیشوند /api/v1 دارند همان را بگذارید، در غیر این صورت "/" قرار دهید
export const api = createClient<paths>({ baseUrl: "/" });
