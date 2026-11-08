"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const LINKS = [
  { href: "/", label: "Control Room" },
  { href: "/how-it-works", label: "How Saga Works" },
];

export function Header() {
  const path = usePathname();
  return (
    <header className="border-b border-slate-800 bg-slate-950/80 backdrop-blur">
      <div className="mx-auto flex max-w-[1500px] flex-wrap items-center justify-between gap-3 px-4 py-3 sm:px-6">
        <div>
          <p className="text-xl font-extrabold text-slate-100">
            Saga Pattern <span className="text-sky-400">Demo</span>
          </p>
          <p className="text-xs text-slate-400">Online Food Ordering · Orchestration · MSS301</p>
        </div>
        <nav aria-label="Main" className="flex gap-2">
          {LINKS.map((l) => (
            <Link
              key={l.href}
              href={l.href}
              aria-current={path === l.href ? "page" : undefined}
              className={`rounded-lg px-4 py-2 text-sm font-semibold transition focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sky-400 ${
                path === l.href ? "bg-sky-600 text-white" : "text-slate-300 hover:bg-slate-800"
              }`}
            >
              {l.label}
            </Link>
          ))}
        </nav>
      </div>
    </header>
  );
}
