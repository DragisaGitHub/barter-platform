import { AxiosError } from "axios";
import type { ErrorResponse } from "../api/generated/types";

export function parseApiError(error: unknown): string {
  if (error instanceof AxiosError) {
    const errorData = error.response?.data as ErrorResponse | undefined;

    if (errorData?.message) {
      return errorData.message;
    }

    if (errorData?.fieldErrors && errorData.fieldErrors.length > 0) {
      return errorData.fieldErrors.map(fe => fe.message).join(", ");
    }

    if (error.message) {
      return error.message;
    }
  }

  if (error instanceof Error) {
    return error.message;
  }

  return "An unexpected error occurred";
}
