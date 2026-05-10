import { type ReactNode } from "react";
import { ChevronUp, ChevronDown } from "lucide-react";
import { cn } from "@/utils";

interface Column<T> {
  key: string;
  label: string;
  sortable?: boolean;
  render: (item: T) => ReactNode;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  data: T[];
  currentSort?: { field: string; direction: "asc" | "desc" } | null;
  onSort?: (field: string) => void;
  onRowClick?: (item: T) => void;
}

export function DataTable<T>({
  columns,
  data = [] as T[],
  currentSort,
  onSort,
  onRowClick,
}: DataTableProps<T>) {
  return (
    <div className="overflow-x-auto rounded-lg border border-slate-200 dark:border-slate-700">
      <table className="w-full">
        <thead className="bg-slate-50 dark:bg-slate-800/50">
          <tr>
            {columns.map((column) => (
              <th
                key={column.key}
                className={cn(
                  "px-6 py-3 text-left text-xs font-medium text-slate-700 dark:text-slate-300 uppercase tracking-wider",
                  column.sortable && onSort && "cursor-pointer select-none hover:bg-slate-100 dark:hover:bg-slate-700/50"
                )}
                onClick={() => column.sortable && onSort?.(column.key)}
              >
                <div className="flex items-center gap-2">
                  {column.label}
                  {column.sortable && currentSort?.field === column.key && (
                    <span className="text-indigo-600 dark:text-indigo-400">
                      {currentSort.direction === "asc" ? (
                        <ChevronUp className="size-4" />
                      ) : (
                        <ChevronDown className="size-4" />
                      )}
                    </span>
                  )}
                </div>
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="bg-white dark:bg-slate-800 divide-y divide-slate-200 dark:divide-slate-700">
          {data.map((item, index) => (
            <tr
              key={index}
              className={cn(
                "transition-colors",
                onRowClick && "cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-700/50"
              )}
              onClick={() => onRowClick?.(item)}
            >
              {columns.map((column) => (
                <td
                  key={column.key}
                  className="px-6 py-4 whitespace-nowrap text-sm text-slate-900 dark:text-slate-100"
                >
                  {column.render(item)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
