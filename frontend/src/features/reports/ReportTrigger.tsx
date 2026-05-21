import { useState, type ReactNode } from "react";
import { Flag } from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";
import type { ReportTargetType } from "@/api/generated/types";
import { useAuth } from "@/auth/AuthContext";
import { Button } from "@/components/ui/Button";
import { buildPathWithQuery, routePaths } from "@/routes/routePaths";
import { ReportDialog } from "./ReportDialog";
import { useTranslation } from "react-i18next";

type ReportTriggerVariant = "primary" | "secondary" | "outline" | "ghost" | "danger";
type ReportTriggerSize = "sm" | "md" | "lg";

interface ReportTriggerProps {
  targetType: ReportTargetType;
  targetUuid: string;
  contextLabel?: string;
  children?: ReactNode;
  className?: string;
  variant?: ReportTriggerVariant;
  size?: ReportTriggerSize;
  disabled?: boolean;
  ariaLabel?: string;
}

export function ReportTrigger({
  targetType,
  targetUuid,
  contextLabel,
  children,
  className,
  variant = "outline",
  size = "sm",
  disabled = false,
  ariaLabel,
}: ReportTriggerProps) {
  const { t } = useTranslation("reporting");
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [isOpen, setIsOpen] = useState(false);

  const handleClick = () => {
    if (disabled) {
      return;
    }

    if (!isAuthenticated) {
      navigate(
        buildPathWithQuery(routePaths.login, {
          redirect: `${location.pathname}${location.search}`,
        }),
      );
      return;
    }

    setIsOpen(true);
  };

  return (
    <>
      <Button
        type="button"
        variant={variant}
        size={size}
        className={className}
        disabled={disabled}
        onClick={handleClick}
        aria-label={ariaLabel}
      >
        <Flag className="size-4" />
        {children ?? t("actions.report")}
      </Button>

      <ReportDialog
        isOpen={isOpen}
        onClose={() => setIsOpen(false)}
        targetType={targetType}
        targetUuid={targetUuid}
        contextLabel={contextLabel}
      />
    </>
  );
}

