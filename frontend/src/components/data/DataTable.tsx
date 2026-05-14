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
    <div className="overflow-x-auto rounded-2xl border border-slate-200 dark:border-slate-800">
      <table className="w-full min-w-[720px]">
        <thead className="bg-slate-50 dark:bg-slate-800/50">
          <tr>
            {columns.map((column) => (
              <th
                key={column.key}
                scope="col"
                aria-sort={
                  column.sortable && currentSort?.field === column.key
                    ? currentSort.direction === "asc"
                      ? "ascending"
                      : "descending"
                    : "none"
                }
                className={cn(
                  "px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-slate-700 dark:text-slate-300"
                )}
              >
                {column.sortable && onSort ? (
                  <button
                    type="button"
                    onClick={() => onSort(column.key)}
                    className="-ml-2 inline-flex items-center gap-2 rounded-lg px-2 py-1 text-left transition-colors hover:bg-slate-100 focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:hover:bg-slate-700/50"
                  >
                    <span>{column.label}</span>
                    {currentSort?.field === column.key ? (
                      <span className="text-indigo-600 dark:text-indigo-400">
                        {currentSort.direction === "asc" ? (
                          <ChevronUp className="size-4" />
                        ) : (
                          <ChevronDown className="size-4" />
                        )}
                      </span>
                    ) : null}
                  </button>
                ) : (
                  <div className="flex items-center gap-2">{column.label}</div>
                )}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-200 bg-white dark:divide-slate-800 dark:bg-slate-900">
          {data.map((item, index) => (
            <tr
              key={index}
              className={cn(
                "transition-colors",
                onRowClick && "cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-800/60"
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
