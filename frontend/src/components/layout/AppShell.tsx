"use client";

import type { ReactNode } from "react";
import Link from "next/link";
import { useSettings } from "@/hooks/use-api";
import { usePathname } from "next/navigation";
import { Droplets, History, LayoutDashboard, Settings } from "lucide-react";

const links = [
  { href: "/", label: "Dashboard", icon: LayoutDashboard },
  { href: "/recipes", label: "Recipes", icon: Droplets },
  { href: "/batches", label: "Batch History", icon: History },
  { href: "/settings", label: "Settings", icon: Settings },
];

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const settings = useSettings();
  const compact = settings.data?.compactLayout;
  return (
    <div className="min-h-screen md:grid md:grid-cols-[240px_1fr]">
      <aside className="hidden border-r border-line bg-white md:flex md:flex-col">
        <div className="border-b border-line px-5 py-5">
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-accent">Tinting</p>
          <p className="mt-1 text-lg font-semibold">Color machine</p>
        </div>
        <nav className="flex flex-1 flex-col gap-1 p-3">
          {links.map((link) => (
            <NavLink key={link.href} {...link} active={isActive(pathname, link.href)} />
          ))}
        </nav>
        <div className="m-3 rounded-lg bg-slate-50 p-3 text-xs leading-5 text-muted">
          Cyan, magenta, yellow, and black are dispensed as separate colorants. Screen color is not a paint formula.
        </div>
      </aside>
      <div className="min-w-0">
        <main className={`mx-auto w-full max-w-6xl px-4 py-4 pb-24 md:px-6 md:pb-6 ${compact ? "md:py-4" : "md:py-6"}`}>{children}</main>
        <nav className="fixed inset-x-0 bottom-0 z-20 grid grid-cols-4 border-t border-line bg-white md:hidden">
          {links.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className={`flex flex-col items-center gap-1 px-2 py-2 text-[11px] ${isActive(pathname, link.href) ? "text-accent" : "text-muted"}`}
            >
              <link.icon size={18} />
              {link.label === "Batch History" ? "History" : link.label}
            </Link>
          ))}
        </nav>
      </div>
    </div>
  );
}

function NavLink({
  href,
  label,
  icon: Icon,
  active,
}: {
  href: string;
  label: string;
  icon: typeof LayoutDashboard;
  active: boolean;
}) {
  return (
    <Link
      href={href}
      className={`flex items-center gap-2 rounded-md px-3 py-2 text-sm ${active ? "bg-teal-50 font-semibold text-accent" : "text-foreground hover:bg-slate-50"}`}
    >
      <Icon size={18} />
      {label}
    </Link>
  );
}

function isActive(pathname: string, href: string) {
  if (href === "/") {
    return pathname === "/";
  }
  return pathname === href || pathname.startsWith(`${href}/`);
}
