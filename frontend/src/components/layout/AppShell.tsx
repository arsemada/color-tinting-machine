"use client";

import type { ReactNode } from "react";
import Link from "next/link";
import { useMachineStatus, useSettings } from "@/hooks/use-api";
import { usePathname } from "next/navigation";
import { Droplets, History, LayoutDashboard, Settings as SettingsIcon, Cpu } from "lucide-react";

const links = [
  { href: "/", label: "Dashboard", icon: LayoutDashboard, desc: "Machine status & controls" },
  { href: "/recipes", label: "Recipes", icon: Droplets, desc: "Color formulas library" },
  { href: "/batches", label: "History", icon: History, desc: "Past batch audit log" },
  { href: "/settings", label: "Settings", icon: SettingsIcon, desc: "Console preferences" },
];

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const settings = useSettings();
  const machine = useMachineStatus();
  const compact = settings.data?.compactLayout;
  const isVerified = machine.data?.hardwareVerified ?? false;

  return (
    <div className="min-h-screen md:grid md:grid-cols-[250px_1fr]">
      <aside className="hidden border-r border-slate-200/90 bg-white/80 backdrop-blur-md md:flex md:flex-col md:justify-between">
        <div>
          {/* Brand header */}
          <div className="border-b border-slate-100 px-6 py-5">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-tr from-teal-700 via-teal-600 to-cyan-500 text-white shadow-md shadow-teal-700/20">
                <Droplets size={22} className="stroke-[2.2]" />
              </div>
              <div>
                <p className="text-sm font-bold tracking-tight text-slate-900">TINTMASTER</p>
                <p className="text-[11px] font-medium uppercase tracking-wider text-teal-700">Operator Console</p>
              </div>
            </div>
          </div>

          {/* Navigation Links */}
          <nav className="flex flex-col gap-1.5 p-3 pt-4">
            {links.map((link) => (
              <NavLink key={link.href} {...link} active={isActive(pathname, link.href)} />
            ))}
          </nav>
        </div>

        {/* Machine hardware indicator card in sidebar bottom */}
        <div className="p-4 border-t border-slate-100 bg-slate-50/50 m-3 rounded-2xl">
          <div className="flex items-center justify-between text-xs font-semibold text-slate-600 mb-2">
            <span className="flex items-center gap-1.5">
              <Cpu size={14} className="text-slate-400" />
              Machine Hardware
            </span>
            <span
              className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-bold ${
                isVerified ? "bg-emerald-100 text-emerald-800" : "bg-slate-200 text-slate-600"
              }`}
            >
              <span className={`h-1.5 w-1.5 rounded-full ${isVerified ? "bg-emerald-500 animate-beacon" : "bg-slate-400"}`} />
              {isVerified ? "LIVE" : "OFFLINE"}
            </span>
          </div>
          <p className="text-[11px] leading-relaxed text-slate-500">
            {isVerified
              ? "Dispenser hardware verified & ready."
              : "Pumps disabled. Awaiting ESP32 controller connection."}
          </p>
        </div>
      </aside>

      {/* Main page content area */}
      <div className="min-w-0 flex flex-col min-h-screen">
        <main className={`mx-auto w-full max-w-6xl px-4 py-5 pb-24 md:px-8 md:pb-10 ${compact ? "md:py-4" : "md:py-8"}`}>
          {children}
        </main>

        {/* Mobile bottom bar */}
        <nav className="fixed inset-x-0 bottom-0 z-30 grid grid-cols-4 border-t border-slate-200 bg-white/95 backdrop-blur-md md:hidden shadow-lg">
          {links.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className={`flex flex-col items-center gap-1 px-2 py-2.5 text-[11px] transition-colors ${
                isActive(pathname, link.href)
                  ? "font-semibold text-teal-700"
                  : "text-slate-500 hover:text-slate-900"
              }`}
            >
              <link.icon size={19} />
              {link.label}
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
      className={`group flex items-center gap-3.5 rounded-xl px-3.5 py-2.5 text-sm font-medium transition-all duration-150 ${
        active
          ? "bg-teal-50 text-teal-800 font-semibold shadow-xs"
          : "text-slate-600 hover:bg-slate-100/70 hover:text-slate-900"
      }`}
    >
      <div
        className={`flex h-8 w-8 items-center justify-center rounded-lg transition-colors ${
          active
            ? "bg-teal-700 text-white shadow-xs"
            : "bg-slate-100 text-slate-500 group-hover:bg-white group-hover:text-slate-800"
        }`}
      >
        <Icon size={17} />
      </div>
      <span>{label}</span>
    </Link>
  );
}

function isActive(pathname: string, href: string) {
  if (href === "/") {
    return pathname === "/";
  }
  return pathname === href || pathname.startsWith(`${href}/`);
}
