# Test Cases

This document highlights all automated test cases currently in the repo. Generated from test sources on disk.

**Frontend E2E (Playwright)**

| File | Test Case |
| --- | --- |
| `frontend/tests/e2e/admin.spec.ts` | E2E-085 users page renders |
| `frontend/tests/e2e/admin.spec.ts` | E2E-086 create user works and E2E-089 deactivate works |
| `frontend/tests/e2e/alerts.spec.ts` | E2E-048 loan alert can be resolved |
| `frontend/tests/e2e/alerts.spec.ts` | E2E-051 global alerts page loads |
| `frontend/tests/e2e/alerts.spec.ts` | E2E-053 global alerts OPEN filter works |
| `frontend/tests/e2e/alerts.spec.ts` | E2E-059 view loan link opens loan alerts with focusAlert |
| `frontend/tests/e2e/auth.spec.ts` | E2E-001 login page loads |
| `frontend/tests/e2e/auth.spec.ts` | E2E-002 seeded analyst login succeeds |
| `frontend/tests/e2e/auth.spec.ts` | E2E-003 invalid login shows error |
| `frontend/tests/e2e/auth.spec.ts` | E2E-004 protected route redirects unauthenticated users to login |
| `frontend/tests/e2e/auth.spec.ts` | E2E-005 session persists across refresh |
| `frontend/tests/e2e/auth.spec.ts` | E2E-006 logout returns user to login screen |
| `frontend/tests/e2e/dashboard.spec.ts` | E2E-021 dashboard loads with seeded loan selector |
| `frontend/tests/e2e/dashboard.spec.ts` | E2E-022 dashboard range tabs update state |
| `frontend/tests/e2e/dashboard.spec.ts` | E2E-024 quick action opens loan directory |
| `frontend/tests/e2e/dashboard.spec.ts` | E2E-025 quick action opens alert center |
| `frontend/tests/e2e/loan-imports.spec.ts` | E2E-090 admin can preview and execute a loan import |
| `frontend/tests/e2e/loan-imports.spec.ts` | E2E-091 analyst cannot access loan imports route |
| `frontend/tests/e2e/loan-imports.spec.ts` | E2E-092 invalid loan import file shows validation errors |
| `frontend/tests/e2e/loan-imports.spec.ts` | E2E-093 admin can review prior loan import history |
| `frontend/tests/e2e/loan-imports.spec.ts` | E2E-094 unchanged loan import row is shown as UNCHANGED |
| `frontend/tests/e2e/loan-imports.spec.ts` | E2E-095 invalid row values are shown as preview row errors |
| `frontend/tests/e2e/loan-imports.spec.ts` | E2E-096 imported loan overview shows external metadata |
| `frontend/tests/e2e/loan-imports.spec.ts` | E2E-097 closed imported loan cannot be reopened through preview |
| `frontend/tests/e2e/loan-results-collateral.spec.ts` | E2E-096 results tab shows evaluation rows and filter works |
| `frontend/tests/e2e/loan-results-collateral.spec.ts` | E2E-097 collateral tab adds collateral and requests an exception |
| `frontend/tests/e2e/loan-workflow.spec.ts` | E2E-038 loan detail tabs are accessible |
| `frontend/tests/e2e/loan-workflow.spec.ts` | E2E-041 overview loads borrower snapshot and covenant list |
| `frontend/tests/e2e/loan-workflow.spec.ts` | E2E-042 add covenant succeeds |
| `frontend/tests/e2e/loan-workflow.spec.ts` | E2E-043 edit existing covenant rule succeeds |
| `frontend/tests/e2e/loan-workflow.spec.ts` | E2E-060 statements tab loads history and submits statement |
| `frontend/tests/e2e/loan-workflow.spec.ts` | E2E-064 statements bulk import works |
| `frontend/tests/e2e/loan-workflow.spec.ts` | E2E-066 documents upload and delete works |
| `frontend/tests/e2e/loan-workflow.spec.ts` | E2E-072 comments add and delete works |
| `frontend/tests/e2e/loan-workflow.spec.ts` | E2E-075 activity tab loads rows |
| `frontend/tests/e2e/loans.spec.ts` | E2E-027 loans table loads seeded rows |
| `frontend/tests/e2e/loans.spec.ts` | E2E-028 search filters loan list |
| `frontend/tests/e2e/loans.spec.ts` | E2E-030 status filter active works |
| `frontend/tests/e2e/loans.spec.ts` | E2E-032 create loan adds a new row |
| `frontend/tests/e2e/loans.spec.ts` | E2E-034 clicking first row navigates to loan overview |
| `frontend/tests/e2e/loans.spec.ts` | E2E-036 close flow updates loan status |
| `frontend/tests/e2e/rbac.spec.ts` | E2E-011 analyst navigation hides privileged areas |
| `frontend/tests/e2e/rbac.spec.ts` | E2E-014 risk lead can access portfolio |
| `frontend/tests/e2e/rbac.spec.ts` | E2E-016 admin can access users page |
| `frontend/tests/e2e/rbac.spec.ts` | E2E-017 analyst direct users route is forbidden |
| `frontend/tests/e2e/rbac.spec.ts` | E2E-019 invalid app route shows not found |
| `frontend/tests/e2e/rbac.spec.ts` | E2E-080 analyst direct portfolio route is forbidden |
| `frontend/tests/e2e/reports-settings.spec.ts` | E2E-082 reports page loads and export history records run |
| `frontend/tests/e2e/reports-settings.spec.ts` | E2E-091 settings page save flow works |

**Backend Integration Tests**

| File | Test Case |
| --- | --- |
| `src/test/java/com/covenantiq/integration/AdditionalCovenantTypesIntegrationTest.java` | evaluatesAllAdditionalCovenantTypes |
| `src/test/java/com/covenantiq/integration/AlertLifecycleIntegrationTest.java` | alertLifecycleOpenToAcknowledgedToUnderReviewToResolved |
| `src/test/java/com/covenantiq/integration/AttachmentIntegrationTest.java` | uploadListDownloadDeleteAttachmentFlow |
| `src/test/java/com/covenantiq/integration/AuthFlowIntegrationTest.java` | loginProtectedEndpointRefreshFlowWorks |
| `src/test/java/com/covenantiq/integration/BulkImportIntegrationTest.java` | bulkImportCsvSuccessAndPartialFailure |
| `src/test/java/com/covenantiq/integration/BulkImportIntegrationTest.java` | bulkImportRejectsLargeFile |
| `src/test/java/com/covenantiq/integration/CommentAndActivityIntegrationTest.java` | commentCreateDeleteAndLoanActivityWorks |
| `src/test/java/com/covenantiq/integration/CommentAndActivityIntegrationTest.java` | globalActivityEndpointRequiresRiskLeadOrAdmin |
| `src/test/java/com/covenantiq/integration/CorsIntegrationTest.java` | preflightAllowsConfiguredOriginForLogin |
| `src/test/java/com/covenantiq/integration/CorsIntegrationTest.java` | preflightRejectsUnknownOriginForLogin |
| `src/test/java/com/covenantiq/integration/CovenantManagementIntegrationTest.java` | covenantCanBeListedAndUpdated |
| `src/test/java/com/covenantiq/integration/EnhancedEarlyWarningIntegrationTest.java` | createsVolatilityAndSeasonalAnomalyAlerts |
| `src/test/java/com/covenantiq/integration/HealthEndpointIntegrationTest.java` | healthEndpointIsUnauthenticatedAndReturnsComponentStatus |
| `src/test/java/com/covenantiq/integration/LoanFlowIntegrationTest.java` | fullLoanFlowWorks |
| `src/test/java/com/covenantiq/integration/LoanImportIntegrationTest.java` | adminCanExecuteBatchWithMixedValidAndInvalidRows |
| `src/test/java/com/covenantiq/integration/LoanImportIntegrationTest.java` | adminCanFetchLoanImportBatchDetail |
| `src/test/java/com/covenantiq/integration/LoanImportIntegrationTest.java` | adminCanListLoanImportHistory |
| `src/test/java/com/covenantiq/integration/LoanImportIntegrationTest.java` | adminCanPreviewAndExecuteLoanImport |
| `src/test/java/com/covenantiq/integration/LoanImportIntegrationTest.java` | adminPreviewFlagsInvalidRowValues |
| `src/test/java/com/covenantiq/integration/LoanImportIntegrationTest.java` | adminPreviewRejectsEmptyFile |
| `src/test/java/com/covenantiq/integration/LoanImportIntegrationTest.java` | adminPreviewRejectsMalformedCsv |
| `src/test/java/com/covenantiq/integration/LoanImportIntegrationTest.java` | adminPreviewRejectsMissingHeaders |
| `src/test/java/com/covenantiq/integration/LoanImportIntegrationTest.java` | analystCannotAccessLoanImportEndpoints |
| `src/test/java/com/covenantiq/integration/LoanImportIntegrationTest.java` | previewRejectsForbiddenClosedToActiveReopen |
| `src/test/java/com/covenantiq/integration/LoanImportIntegrationTest.java` | reimportOfSameDataReturnsUnchangedAndDoesNotCreateDuplicateLoan |
| `src/test/java/com/covenantiq/integration/LoanImportIntegrationTest.java` | reimportUpdatesExistingLoanInsteadOfCreatingAnotherLoan |
| `src/test/java/com/covenantiq/integration/RbacEnforcementIntegrationTest.java` | analystCanAccessPortfolioSummary |
| `src/test/java/com/covenantiq/integration/RbacEnforcementIntegrationTest.java` | riskLeadCannotCreateLoanAndAnalystCannotResolveAlert |
| `src/test/java/com/covenantiq/integration/UserManagementIntegrationTest.java` | adminCanManageUsersAndAnalystCannot |

**Backend Service Tests**

| File | Test Case |
| --- | --- |
| `src/test/java/com/covenantiq/service/AlertServiceTest.java` | acknowledgeOpenAlertAsAnalyst |
| `src/test/java/com/covenantiq/service/AlertServiceTest.java` | cannotTransitionFromResolvedToOpen |
| `src/test/java/com/covenantiq/service/AlertServiceTest.java` | cannotTransitionOpenDirectlyToUnderReview |
| `src/test/java/com/covenantiq/service/AlertServiceTest.java` | resolveAcknowledgedAlertWithNotesAsRiskLead |
| `src/test/java/com/covenantiq/service/AlertServiceTest.java` | resolveForbiddenForUnauthorizedRole |
| `src/test/java/com/covenantiq/service/AlertServiceTest.java` | resolveRequiresResolutionNotes |
| `src/test/java/com/covenantiq/service/AlertServiceTest.java` | updateStatusThrowsWhenAlertIsSuperseded |
| `src/test/java/com/covenantiq/service/AlertServiceTest.java` | updateStatusThrowsWhenAlertNotFound |
| `src/test/java/com/covenantiq/service/AuthServiceTest.java` | loginFailedPasswordIncrementsAttempts |
| `src/test/java/com/covenantiq/service/AuthServiceTest.java` | loginFailedPasswordLocksAccountAfterFiveAttempts |
| `src/test/java/com/covenantiq/service/AuthServiceTest.java` | loginFailsForInactiveAccount |
| `src/test/java/com/covenantiq/service/AuthServiceTest.java` | loginFailsWhenAccountIsLocked |
| `src/test/java/com/covenantiq/service/AuthServiceTest.java` | loginReturnsTokensAndResetsFailedAttempts |
| `src/test/java/com/covenantiq/service/AuthServiceTest.java` | refreshFailsForInactiveUser |
| `src/test/java/com/covenantiq/service/AuthServiceTest.java` | refreshFailsWhenTokenCannotBeParsed |
| `src/test/java/com/covenantiq/service/AuthServiceTest.java` | refreshFailsWhenTokenTypeIsNotRefresh |
| `src/test/java/com/covenantiq/service/AuthServiceTest.java` | refreshReturnsNewAccessTokenWhenRefreshTokenIsValid |
| `src/test/java/com/covenantiq/service/ChangeControlServiceTest.java` | rollbackRejectsTargetFromDifferentChangeRequestLineage |
| `src/test/java/com/covenantiq/service/CovenantEvaluationServiceTest.java` | createsPassAndBreachResultsAndAlert |
| `src/test/java/com/covenantiq/service/ExportServiceTest.java` | exportAlertsCsvIncludesExpectedHeaderAndEscapedFields |
| `src/test/java/com/covenantiq/service/ExportServiceTest.java` | exportCovenantResultsCsvFormatsDecimalsAndPeriodSuffix |
| `src/test/java/com/covenantiq/service/ExternalLoanSyncServiceTest.java` | closesActiveLoanWhenImportedStatusIsClosed |
| `src/test/java/com/covenantiq/service/ExternalLoanSyncServiceTest.java` | createsLoanWhenNoExistingMatchIsFound |
| `src/test/java/com/covenantiq/service/ExternalLoanSyncServiceTest.java` | preservesUserManagedFieldsWhenUpdatingImportedLoan |
| `src/test/java/com/covenantiq/service/ExternalLoanSyncServiceTest.java` | rejectsClosedToActiveReopenWhenReopenIsNotAllowed |
| `src/test/java/com/covenantiq/service/ExternalLoanSyncServiceTest.java` | returnsUnchangedWhenImportedRowHasNoEffectiveFieldChanges |
| `src/test/java/com/covenantiq/service/ExternalLoanSyncServiceTest.java` | updatesExistingLoanWhenMatchingSourceSystemAndExternalLoanIdExists |
| `src/test/java/com/covenantiq/service/FinancialRatioServiceTest.java` | calculatesAllPhase2Ratios |
| `src/test/java/com/covenantiq/service/FinancialRatioServiceTest.java` | calculatesCurrentRatioWithScale |
| `src/test/java/com/covenantiq/service/FinancialRatioServiceTest.java` | calculatesDebtToEquity |
| `src/test/java/com/covenantiq/service/FinancialRatioServiceTest.java` | throwsWhenCurrentLiabilitiesZero |
| `src/test/java/com/covenantiq/service/FinancialRatioServiceTest.java` | throwsWhenDscrDenominatorZero |
| `src/test/java/com/covenantiq/service/LoanImportServiceTest.java` | executeMarksRowsUnchangedWhenNoDataChangesExist |
| `src/test/java/com/covenantiq/service/LoanImportServiceTest.java` | executeProcessesValidRowsAndReportsInvalidRows |
| `src/test/java/com/covenantiq/service/LoanImportServiceTest.java` | previewFlagsInvalidRowValues |
| `src/test/java/com/covenantiq/service/LoanImportServiceTest.java` | previewMarksExistingLoanAsUnchangedWhenImportedDataMatches |
| `src/test/java/com/covenantiq/service/LoanImportServiceTest.java` | previewMarksExistingLoanAsUpdateWhenImportedDataDiffers |
| `src/test/java/com/covenantiq/service/LoanImportServiceTest.java` | previewRejectsEmptyFile |
| `src/test/java/com/covenantiq/service/LoanImportServiceTest.java` | previewRejectsMalformedCsv |
| `src/test/java/com/covenantiq/service/LoanImportServiceTest.java` | previewRejectsMissingRequiredHeaders |
| `src/test/java/com/covenantiq/service/PortfolioSummaryServiceTest.java` | getSummaryAggregatesActiveLoanRiskAndAlertCounts |
| `src/test/java/com/covenantiq/service/PortfolioSummaryServiceTest.java` | getTrendBuildsQuarterlyPortfolioRiskSeriesFromHistoricalStatements |
| `src/test/java/com/covenantiq/service/RiskSummaryServiceTest.java` | getRiskDetailsMapsCovenantsAndTriggeredRuleCodes |
| `src/test/java/com/covenantiq/service/RiskSummaryServiceTest.java` | getRiskDetailsReturnsEmptyPayloadWhenNoStatementsExist |
| `src/test/java/com/covenantiq/service/RiskSummaryServiceTest.java` | getRiskSummaryReturnsHighWhenAnyHighSeverityBreachExists |
| `src/test/java/com/covenantiq/service/RiskSummaryServiceTest.java` | getRiskSummaryReturnsLowWhenNoBreachesAndNoWarnings |
| `src/test/java/com/covenantiq/service/RiskSummaryServiceTest.java` | getRiskSummaryReturnsLowWhenNoStatementsExist |
| `src/test/java/com/covenantiq/service/RiskSummaryServiceTest.java` | getRiskSummaryReturnsMediumWhenNoHighBreachButWarningsExist |
| `src/test/java/com/covenantiq/service/TrendAnalysisServiceTest.java` | createsSeasonalAnomalyAlertWhenDeviationExceedsThreshold |
| `src/test/java/com/covenantiq/service/TrendAnalysisServiceTest.java` | createsVolatilityAlertWhenStdDevExceedsThreshold |
| `src/test/java/com/covenantiq/service/TrendAnalysisServiceTest.java` | skipsSeasonalAnomalyForAnnualStatements |
| `src/test/java/com/covenantiq/service/TrendAnalysisServiceTest.java` | skipsVolatilityWhenLessThanFourStatements |
| `src/test/java/com/covenantiq/service/WebhookSecretCodecTest.java` | decodeSupportsLegacyBase64Secrets |
| `src/test/java/com/covenantiq/service/WebhookSecretCodecTest.java` | encodeAndDecodeRoundTripUsesVersionedCiphertext |
| `src/test/java/com/covenantiq/service/WebhookSecretCodecTest.java` | encodeUsesRandomIvSoCiphertextDiffersForSameInput |

**Backend Config/Security/DTO Tests**

| File | Test Case |
| --- | --- |
| `src/test/java/com/covenantiq/config/RuntimeModeValidatorTest.java` | allowsPlaceholderSecretsWhenDemoModeIsEnabled |
| `src/test/java/com/covenantiq/config/RuntimeModeValidatorTest.java` | rejectsConflictingDemoAndTestModes |
| `src/test/java/com/covenantiq/config/RuntimeModeValidatorTest.java` | rejectsPlaceholderSecretsWhenStrictValidationIsEnabled |
| `src/test/java/com/covenantiq/dto/request/SubmitFinancialStatementRequestValidationTest.java` | rejectsNegativeExtendedMonetaryFields |
| `src/test/java/com/covenantiq/security/CorrelationIdFilterTest.java` | generatesCorrelationIdWhenMissingAndClearsMdcAfterCompletion |
| `src/test/java/com/covenantiq/security/CorrelationIdFilterTest.java` | reusesIncomingCorrelationIdAndPopulatesMdcDuringRequest |
