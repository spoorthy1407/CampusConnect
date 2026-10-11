import { API } from "./api";

export const fmt = (d) => new Date(d).toLocaleString([], { dateStyle: "medium", timeStyle: "short" });
export const money = (n) => "₹" + Number(n).toLocaleString("en-IN");
export const imgUrl = (u) => (u ? API + u : null);
export const hhmm = (t) => (t ? String(t).slice(0, 5) : "");
export const label = (s) => String(s || "").replaceAll("_", " ");
export const isStaff = (u) => !!u && (u.role === "ADMIN" || u.role === "SUPER_ADMIN");
export const Badge = ({ s }) => <span className={`badge ${s}`}>{label(s)}</span>;
export const Err = ({ msg }) => (msg ? <p className="error">{msg}</p> : null);
export const Flash = ({ msg }) => (msg ? <p className="flash">{msg}</p> : null);

export const TYPE_META = {
  LIBRARY: { label: "Libraries", icon: "📚" },
  INTERACTIVE_SPACE: { label: "Interactive Space", icon: "💬" },
  PRINTING: { label: "Printing", icon: "🖨️" },
  TUTORING: { label: "Tutoring", icon: "🎓" },
  SPORTS_COURT: { label: "Sports Courts", icon: "🏸" },
};
export const TYPE_ORDER = ["LIBRARY", "INTERACTIVE_SPACE", "PRINTING", "TUTORING", "SPORTS_COURT"];

export const pad = (n) => String(n).padStart(2, "0");
export const todayStr = () => {
  const d = new Date();
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
};

export const sizeLabel = (bytes) =>
  bytes > 1024 * 1024 ? `${(bytes / 1024 / 1024).toFixed(1)} MB` : `${Math.max(1, Math.round(bytes / 1024))} KB`;

export function FacilityInfo({ f }) {
  return (
    <div className="note">
      <b>{f.name}</b>{f.blockName ? ` · ${f.blockName}` : ""}
      <div>🕒 Open {hhmm(f.openTime)} – {hhmm(f.closeTime)}{f.type !== "PRINTING" && f.maxBookingMinutes ? ` · Max ${f.maxBookingMinutes} min per booking` : ""}</div>
      {f.description && <div className="muted">{f.description}</div>}
    </div>
  );
}
