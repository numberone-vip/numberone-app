import { createServerClient } from "@supabase/ssr";
import { cookies } from "next/headers";

export async function createClient() {
  const jar = await cookies();
  const url = process.env.NEXT_PUBLIC_SUPABASE_URL;
  const key = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY;
  if (!url || !key) throw new Error("Missing Supabase environment variables");
  return createServerClient(url, key, {
    cookies: {
      getAll() { return jar.getAll(); },
      setAll(values: Array<{ name: string; value: string; options?: Parameters<typeof jar.set>[2] }>) {
        try {
          values.forEach(({ name, value, options }) => jar.set(name, value, options));
        } catch {
          // Server Components cannot write cookies; middleware refreshes them.
        }
      }
    }
  });
}
