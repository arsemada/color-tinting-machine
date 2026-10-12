import type { ApiErrorBody, CommandResult } from "@/lib/api/types";

export class ApiError extends Error {
  status: number;
  errorCode: string;
  fieldErrors: { field: string; message: string }[];
  correlationId?: string;

  constructor(body: ApiErrorBody) {
    super(body.message || "Request failed");
    this.status = body.status;
    this.errorCode = body.errorCode;
    this.fieldErrors = body.fieldErrors ?? [];
    this.correlationId = body.correlationId;
  }
}

export function apiBaseUrl(): string {
  const configured = process.env.NEXT_PUBLIC_API_BASE_URL?.replace(/\/$/, "");
  return configured || "http://localhost:8080";
}

function isCommand(value: unknown): value is CommandResult {
  return Boolean(
    value &&
      typeof value === "object" &&
      "commandId" in value &&
      "lifecycle" in value &&
      typeof (value as CommandResult).lifecycle === "string",
  );
}

export async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${apiBaseUrl()}${path}`, {
      ...init,
      headers: {
        Accept: "application/json",
        ...(init?.body ? { "Content-Type": "application/json" } : {}),
        ...init?.headers,
      },
    });
  } catch {
    throw new ApiError({
      status: 0,
      errorCode: "BACKEND_UNAVAILABLE",
      message: "The backend is unavailable. Check that the API is running.",
    });
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  const payload = text ? (JSON.parse(text) as unknown) : null;
  if (!response.ok) {
    if (isCommand(payload)) {
      return payload as T;
    }
    const body = (payload ?? {}) as Partial<ApiErrorBody>;
    throw new ApiError({
      status: response.status,
      errorCode: body.errorCode ?? "REQUEST_FAILED",
      message: body.message ?? `Request failed with status ${response.status}.`,
      correlationId: body.correlationId,
      fieldErrors: body.fieldErrors ?? [],
    });
  }
  return payload as T;
}
