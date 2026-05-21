import { useEffect, useMemo, useState } from "react";
import { AlertTriangle, CheckCircle2, ShieldAlert } from "lucide-react";
import type { ReportReasonCode, ReportTargetType } from "@/api/generated/types";
import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";
import { parseApiError } from "@/utils";
import { useCreateReport } from "./useReports";
import {
  REPORT_REASON_OPTIONS,
  reportReasonTranslationKey,
  reportTargetTypeTranslationKey,
} from "./reporting";
import { useTranslation } from "react-i18next";

interface ReportDialogProps {
  isOpen: boolean;
  onClose: () => void;
  targetType: ReportTargetType;
  targetUuid: string;
  contextLabel?: string;
}

const MAX_DETAILS_LENGTH = 2000;

export function ReportDialog({
  isOpen,
  onClose,
  targetType,
  targetUuid,
  contextLabel,
}: ReportDialogProps) {
  const { t } = useTranslation(["reporting", "common"]);
  const createReport = useCreateReport();
  const [reasonCode, setReasonCode] = useState<ReportReasonCode | "">("");
  const [details, setDetails] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const trimmedDetails = details.trim();

  const validationError = useMemo(() => {
    if (!reasonCode) {
      return t("reporting:dialog.validationReason");
    }

    if (details.length > MAX_DETAILS_LENGTH) {
      return t("reporting:dialog.validationDetailsTooLong");
    }

    return null;
  }, [details.length, reasonCode, t]);

  const resetState = () => {
    setReasonCode("");
    setDetails("");
    setError(null);
    setSuccessMessage(null);
  };

  useEffect(() => {
    if (!isOpen) {
      resetState();
    }
  }, [isOpen]);

  const handleClose = () => {
    if (createReport.isPending) {
      return;
    }

    resetState();
    onClose();
  };

  const handleSubmit = async () => {
    setError(null);

    if (validationError) {
      setError(validationError);
      return;
    }

    try {
      const response = await createReport.mutateAsync({
        targetType,
        targetUuid,
        reasonCode: reasonCode as ReportReasonCode,
        details: trimmedDetails ? trimmedDetails : null,
      });

      setSuccessMessage(response.message?.trim() || t("reporting:dialog.successDescription"));
    } catch (submitError) {
      setError(parseApiError(submitError));
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={handleClose} title={t("reporting:dialog.title")} size="md">
      {successMessage ? (
        <div className="space-y-5">
          <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-5 text-center dark:border-emerald-900/60 dark:bg-emerald-950/20">
            <div className="mx-auto flex size-12 items-center justify-center rounded-full bg-white text-emerald-600 shadow-sm dark:bg-slate-900 dark:text-emerald-300">
              <CheckCircle2 className="size-6" />
            </div>
            <h3 className="mt-3 text-lg font-semibold text-slate-900 dark:text-slate-100">
              {t("reporting:dialog.successTitle")}
            </h3>
            <p className="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-300">{successMessage}</p>
          </div>

          <div className="flex justify-end">
            <Button type="button" onClick={handleClose}>
              {t("reporting:dialog.close")}
            </Button>
          </div>
        </div>
      ) : (
        <div className="space-y-5">
          <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-700 dark:bg-slate-900/60">
            <div className="flex items-start gap-3">
              <div className="flex size-10 shrink-0 items-center justify-center rounded-full bg-indigo-100 text-indigo-600 dark:bg-indigo-950/50 dark:text-indigo-300">
                <ShieldAlert className="size-5" />
              </div>
              <div>
                <p className="text-sm font-medium text-slate-900 dark:text-slate-100">
                  {t("reporting:dialog.subtitle")}
                </p>
                {contextLabel ? (
                  <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{contextLabel}</p>
                ) : null}
              </div>
            </div>
          </div>

          <label className="block">
            <span className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-200">
              {t("reporting:dialog.targetType")}
            </span>
            <input
              type="text"
              value={t(`reporting:${reportTargetTypeTranslationKey(targetType)}`)}
              readOnly
              className="w-full rounded-lg border border-slate-300 bg-slate-100 px-3 py-2 text-sm text-slate-700 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-200"
            />
          </label>

          <label className="block">
            <span className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-200">
              {t("reporting:dialog.reason")}
            </span>
            <select
              value={reasonCode}
              onChange={(event) => setReasonCode(event.target.value as ReportReasonCode | "")}
              disabled={createReport.isPending}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            >
              <option value="">{t("reporting:dialog.selectReason")}</option>
              {REPORT_REASON_OPTIONS.map((option) => (
                <option key={option} value={option}>
                  {t(`reporting:${reportReasonTranslationKey(option)}`)}
                </option>
              ))}
            </select>
          </label>

          <label className="block">
            <span className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-200">
              {t("reporting:dialog.details")} {t("common:optionalParenthesized")}
            </span>
            <textarea
              value={details}
              onChange={(event) => setDetails(event.target.value)}
              disabled={createReport.isPending}
              rows={5}
              maxLength={MAX_DETAILS_LENGTH}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
              placeholder={t("reporting:dialog.detailsPlaceholder")}
            />
            <div className="mt-2 flex items-center justify-between gap-3 text-xs text-slate-500 dark:text-slate-400">
              <span>{t("reporting:dialog.detailsHelper")}</span>
              <span>
                {details.length}/{MAX_DETAILS_LENGTH}
              </span>
            </div>
          </label>

          {error ? (
            <div className="flex items-start gap-2 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700 dark:border-red-900/60 dark:bg-red-950/20 dark:text-red-300">
              <AlertTriangle className="mt-0.5 size-4 shrink-0" />
              <span>{error}</span>
            </div>
          ) : null}

          <div className="flex justify-end gap-3">
            <Button type="button" variant="outline" onClick={handleClose} disabled={createReport.isPending}>
              {t("common:cancel")}
            </Button>
            <Button type="button" onClick={handleSubmit} isLoading={createReport.isPending}>
              {t("reporting:dialog.submit")}
            </Button>
          </div>
        </div>
      )}
    </Modal>
  );
}

