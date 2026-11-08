export type SagaStatus =
  "PENDING" | "RUNNING" | "COMPLETED" | "FAILED" | "COMPENSATING" | "COMPENSATED" | "COMPENSATION_FAILED";

export type StepStatus =
  "PENDING" | "RUNNING" | "COMPLETED" | "FAILED" | "COMPENSATED" | "COMPENSATION_FAILED" | "SKIPPED";

export interface FailureSimulation {
  failCreateOrder: boolean;
  failReserveInventory: boolean;
  failProcessPayment: boolean;
  failConfirmOrder: boolean;
  failReleaseInventory: boolean;
  failRefundPayment: boolean;
  failCancelOrder: boolean;
}

export interface StartSagaRequest {
  customerName: string;
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  failureSimulation: FailureSimulation;
}

export interface StartSagaResponse {
  success: boolean;
  sagaId: string;
  status: string;
  pollUrl: string;
  message: string;
}

export interface MenuItem {
  productId: string;
  productName: string;
  totalUnits: number;
  reservedUnits: number;
  availableUnits: number;
  unitPrice: number;
}

export interface SagaStep {
  step: string;
  type: "FORWARD" | "COMPENSATION";
  status: StepStatus;
  targetService: string;
  compensationFor: string | null;
  sequence: number;
  startedAt: string | null;
  completedAt: string | null;
  durationMs: number;
  attempts: number;
  errorMessage: string | null;
}

export interface ServiceCall {
  sequence: number;
  step: string | null;
  stepType: "FORWARD" | "COMPENSATION" | null;
  sourceService: string;
  targetService: string;
  method: string;
  endpoint: string;
  responseStatus: number | null;
  outcome: string;
  requestSummary: string | null;
  responseSummary: string | null;
  timestamp: string;
  durationMs: number;
}

export interface SagaLog {
  sequence: number;
  level: "INFO" | "SUCCESS" | "WARN" | "ERROR" | string;
  phase: string;
  message: string;
  timestamp: string;
}

export interface SystemState {
  label: string;
  productId: string;
  productName: string | null;
  inventoryTotalUnits: number | null;
  inventoryReservedUnits: number | null;
  inventoryAvailableUnits: number | null;
  paymentStatus: string;
  paymentReference: string | null;
  orderStatus: string;
  capturedAt: string;
}

export interface SagaDetail {
  sagaId: string;
  orderId: string | null;
  status: SagaStatus;
  currentStep: string | null;
  customerName: string;
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  totalAmount: number;
  reservationId: string | null;
  paymentId: string | null;
  failedStep: string | null;
  failureReason: string | null;
  compensationRequired: boolean;
  manualRecoveryRequired: boolean;
  recoveryHint: string | null;
  startedAt: string;
  completedAt: string | null;
  durationMs: number;
  finished: boolean;
  steps: SagaStep[];
  serviceCalls: ServiceCall[];
  logs: SagaLog[];
  stateBefore: SystemState | null;
  stateAfter: SystemState | null;
  stateNow: SystemState | null;
}

export interface SagaSummary {
  sagaId: string;
  orderId: string | null;
  status: SagaStatus;
  customerName: string;
  productName: string;
  quantity: number;
  totalAmount: number;
  failedStep: string | null;
  manualRecoveryRequired: boolean;
  startedAt: string;
  durationMs: number;
}

export interface ServiceHealth {
  service: string;
  displayName: string;
  baseUrl: string;
  status: "ONLINE" | "OFFLINE" | string;
  responseTimeMs: number | null;
  database: string;
  error: string | null;
}

export interface SagaDefinition {
  sagaStyle: string;
  forwardSteps: {
    step: string;
    targetService: string;
    compensation: string;
    compensationService: string;
    hasCompensation: boolean;
  }[];
  compensationMap: { forwardStep: string; compensationStep: string; targetService: string }[];
  sagaStates: string[];
  stepStates: string[];
}
