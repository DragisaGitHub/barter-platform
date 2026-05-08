import { Outlet } from "react-router-dom";

export function PublicLayout() {
  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-900">
      <Outlet />
    </div>
  );
}
