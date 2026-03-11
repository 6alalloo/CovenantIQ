export type LoanImportBatchStatus = "PREVIEW_READY" | "COMPLETED" | "FAILED";
export type LoanImportRowAction = "CREATE" | "UPDATE" | "UNCHANGED" | "ERROR";

export type LoanImportBatch = {
  id: number;
  fileName: string;
  uploadedBy: string;
  startedAt: string;
  completedAt: string | null;
  status: LoanImportBatchStatus;
  totalRows: number;
  validRows: number;
  invalidRows: number;
  createdCount: number;
  updatedCount: number;
  unchangedCount: number;
  failedCount: number;
  sourceSystem: string | null;
};

export type LoanImportRow = {
  id: number;
  rowNumber: number;
  sourceSystem: string | null;
  externalLoanId: string | null;
  borrowerName: string | null;
  action: LoanImportRowAction;
  validationMessage: string | null;
  loanId: number | null;
};

export type LoanImportPreviewResponse = {
  batch: LoanImportBatch;
  rows: LoanImportRow[];
};

export type LoanImportExecuteResponse = {
  batch: LoanImportBatch;
  rows: LoanImportRow[];
};
