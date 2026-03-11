export type BackendMode = "NORMAL" | "DEMO" | "TEST";

export type RuntimeConfig = {
  backendMode: BackendMode;
  demoMode: boolean;
  testMode: boolean;
  sampleContentAvailable: boolean;
  strictSecretValidationEnabled: boolean;
};

export type ProblemDetails = {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  correlationId?: string;
  code?: string;
};

export type PageResponse<T> = {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
};
