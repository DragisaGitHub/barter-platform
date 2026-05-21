import { useMutation } from "@tanstack/react-query";
import { createReport } from "@/api/reportsApi";
import type { CreateReportRequest, MessageResponse } from "@/api/generated/types";

export function useCreateReport() {
  return useMutation<MessageResponse, unknown, CreateReportRequest>({
    mutationFn: createReport,
  });
}

