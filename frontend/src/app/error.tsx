"use client";

export default function GlobalError({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return (
    <div role="alert" className="mx-auto max-w-xl px-6 py-16 text-center">
      <h1 className="mb-2 text-2xl font-extrabold text-slate-100">Something went wrong</h1>
      <p className="mb-6 text-sm text-slate-400">{error.message || "An unexpected error occurred in the page."}</p>
      <button
        type="button"
        onClick={reset}
        className="rounded-lg bg-sky-600 px-5 py-2.5 font-bold text-white hover:bg-sky-500 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sky-300"
      >
        Try again
      </button>
    </div>
  );
}
