import type { LoanImportBatch, LoanImportExecuteResponse, LoanImportPreviewResponse, LoanImportRow } from "../types/api";
import { request } from "./core/http";

export function previewLoanImport(file: File) {
  const body = new FormData();
  body.append("file", file);
  return request<LoanImportPreviewResponse>("/admin/loan-imports/preview", {
    method: "POST",
    body,
  });
}

export function runLoanImport(batchId: number) {
  return request<LoanImportExecuteResponse>(`/admin/loan-imports/${batchId}/execute`, {
    method: "POST",
  });
}

export function getLoanImports() {
  return request<LoanImportBatch[]>("/admin/loan-imports");
}

export function getLoanImport(batchId: number) {
  return request<LoanImportBatch>(`/admin/loan-imports/${batchId}`);
}

export function getLoanImportRows(batchId: number) {
  return request<LoanImportRow[]>(`/admin/loan-imports/${batchId}/rows`);
}
