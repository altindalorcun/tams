import { DepartmentsTab } from "./DepartmentsTab";

/**
 * Standalone admin page for department CRUD, linked from the sidebar.
 */
export function DepartmentsPage() {
  return (
    <div className="max-w-7xl mx-auto px-6 py-8 space-y-8">
      <h1 className="sr-only">Bölümler</h1>
      <DepartmentsTab />
    </div>
  );
}
