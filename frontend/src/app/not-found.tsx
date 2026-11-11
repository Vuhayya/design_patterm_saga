import Link from "next/link";

export default function NotFound() {
  return (
    <div className="mx-auto max-w-xl px-6 py-16 text-center">
      <h1 className="mb-2 text-2xl font-extrabold text-slate-100">Không tìm thấy trang</h1>
      <p className="mb-6 text-sm text-slate-400">Trang này không tồn tại trong demo.</p>
      <Link href="/" className="font-bold text-sky-400 underline-offset-4 hover:underline">
        Về Control Room
      </Link>
    </div>
  );
}
