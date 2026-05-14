import { type ReactNode } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "../ui/Button";
import { cn } from "@/utils";

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  statusContent?: ReactNode;
  className?: string;
}

export function Pagination({
  currentPage,
  totalPages,
  onPageChange,
  statusContent,
  className,
}: PaginationProps) {
  const pages = [];
  const maxVisible = 5;
  const hasMultiplePages = totalPages > 1;

  let start = Math.max(0, currentPage - Math.floor(maxVisible / 2));
  let end = Math.min(totalPages, start + maxVisible);

  if (end - start < maxVisible) {
    start = Math.max(0, end - maxVisible);
  }

  for (let i = start; i < end; i++) {
    pages.push(i);
  }

  return (
    <div className={cn("border-t border-slate-200 px-4 py-3 dark:border-slate-700", className)}>
      <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div className="flex flex-1 flex-col gap-3 md:flex-row md:items-center">
          {statusContent && <div className="flex flex-wrap items-center gap-2">{statusContent}</div>}

          {hasMultiplePages && (
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => onPageChange(currentPage - 1)}
                disabled={currentPage === 0}
              >
                <ChevronLeft className="size-4" />
                Previous
              </Button>

              <div className="flex items-center gap-1">
                {pages.map((page) => (
                  <button
                    key={page}
                    onClick={() => onPageChange(page)}
                    className={`rounded-lg px-3 py-1.5 text-sm transition-colors ${
                      page === currentPage
                        ? "bg-indigo-600 text-white"
                        : "text-slate-700 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-700"
                    }`}
                  >
                    {page + 1}
                  </button>
                ))}
              </div>

              <Button
                variant="outline"
                size="sm"
                onClick={() => onPageChange(currentPage + 1)}
                disabled={currentPage === totalPages - 1}
              >
                Next
                <ChevronRight className="size-4" />
              </Button>
            </div>
          )}
        </div>

        <p className="text-sm text-slate-600 dark:text-slate-400">
          Page {currentPage + 1} of {totalPages}
        </p>
      </div>
    </div>
  );
}
