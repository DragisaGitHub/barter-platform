import { Card } from "../../components/ui/Card";
import { Skeleton } from "../../components/ui/Skeleton";

export function PublicProfilePageSkeleton() {
  return (
    <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
      <Skeleton className="mb-6 h-5 w-40" />

      <Card className="overflow-hidden border-violet-100/80 bg-gradient-to-br from-violet-50 via-white to-white p-0 shadow-sm dark:border-violet-900/40 dark:from-violet-950/20 dark:via-slate-900 dark:to-slate-900">
        <div className="flex flex-col gap-6 p-6 lg:flex-row lg:items-center lg:justify-between lg:p-8">
          <div className="flex flex-1 flex-col gap-5 sm:flex-row sm:items-center">
            <Skeleton className="size-20 rounded-full" />
            <div className="min-w-0 flex-1 space-y-3">
              <Skeleton className="h-9 w-48" />
              <Skeleton className="h-4 w-64 max-w-full" />
              <div className="flex flex-wrap gap-2">
                <Skeleton className="h-8 w-28 rounded-full" />
                <Skeleton className="h-8 w-32 rounded-full" />
                <Skeleton className="h-8 w-24 rounded-full" />
              </div>
            </div>
          </div>

          <div className="grid w-full gap-3 sm:grid-cols-2 lg:w-[320px]">
            <Skeleton className="h-24 rounded-2xl" />
            <Skeleton className="h-24 rounded-2xl" />
          </div>
        </div>
      </Card>

      <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {Array.from({ length: 4 }).map((_, index) => (
          <Card key={index} className="space-y-3 rounded-2xl p-5">
            <Skeleton className="h-10 w-10 rounded-2xl" />
            <Skeleton className="h-4 w-28" />
            <Skeleton className="h-8 w-20" />
            <Skeleton className="h-4 w-full" />
          </Card>
        ))}
      </div>

      <div className="mt-8 flex items-end justify-between gap-4">
        <div className="space-y-2">
          <Skeleton className="h-7 w-44" />
          <Skeleton className="h-4 w-72 max-w-full" />
        </div>
        <Skeleton className="hidden h-9 w-24 rounded-lg sm:block" />
      </div>

      <div className="mt-5 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {Array.from({ length: 8 }).map((_, index) => (
          <Card key={index} className="overflow-hidden border-slate-200/80 p-0 shadow-sm dark:border-slate-700/80">
            <Skeleton className="aspect-[4/3] w-full rounded-none" />
            <div className="space-y-3 p-4">
              <div className="flex gap-2">
                <Skeleton className="h-5 w-16 rounded-full" />
                <Skeleton className="h-5 w-20 rounded-full" />
              </div>
              <Skeleton className="h-5 w-full" />
              <Skeleton className="h-5 w-3/4" />
              <Skeleton className="h-4 w-28" />
              <Skeleton className="h-4 w-32" />
              <Skeleton className="h-4 w-24" />
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
}

