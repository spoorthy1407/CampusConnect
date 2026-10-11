export const API = import.meta.env.VITE_API_URL || "http://localhost:8080";

export async function api(path, { method = "GET", body, form } = {}) {
  const token = localStorage.getItem("token");
  const headers = {};
  if (token && !path.startsWith("/api/auth")) headers.Authorization = `Bearer ${token}`;
  let payload;
  if (form) payload = form;
  else if (body) {
    headers["Content-Type"] = "application/json";
    payload = JSON.stringify(body);
  }
  const res = await fetch(API + path, { method, headers, body: payload });
  if (res.status === 401 && token && !path.startsWith("/api/auth")) {
    localStorage.clear();
    window.location.href = "/login";
    throw new Error("Session expired");
  }
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }
  if (!res.ok) throw new Error(data?.message || "Request failed");
  return data;
}

export async function download(path, fileName) {
  const token = localStorage.getItem("token");
  const res = await fetch(API + path, { headers: token ? { Authorization: `Bearer ${token}` } : {} });
  if (!res.ok) {
    let message = "Download failed";
    try { message = (await res.json()).message || message; } catch {}
    throw new Error(message);
  }
  const url = URL.createObjectURL(await res.blob());
  const a = document.createElement("a");
  a.href = url;
  a.download = fileName;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}
