import { CoursesTab } from "./CoursesTab";

/**
 * Standalone admin page for course catalog CRUD, linked from the sidebar.
 */
export function CoursesPage() {
  return (
    <div className="max-w-7xl mx-auto px-6 py-8 space-y-8">
      <h1 className="sr-only">Dersler</h1>
      <CoursesTab />
    </div>
  );
}
