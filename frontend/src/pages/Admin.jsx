import { useCallback, useEffect, useState } from "react";
import { api } from "../api";
import { useAuth } from "../AuthContext";
import { Err } from "../ui";
import BookingsTab from "./admin/BookingsTab";
import PrintTab from "./admin/PrintTab";
import ServicesTab from "./admin/ServicesTab";
import FacilitiesTab from "./admin/FacilitiesTab";
import AdminsTab from "./admin/AdminsTab";
import MarketTab from "./admin/MarketTab";
import StatsTab from "./admin/StatsTab";

export default function Admin() {
  const { user } = useAuth();
  const isSuper = user.role === "SUPER_ADMIN";
  const [scope, setScope] = useState(null);
  const [tab, setTab] = useState("bookings");
  const [err, setErr] = useState("");

  const loadScope = useCallback(() => {
    api("/api/admin/scope").then(setScope).catch((e) => setErr(e.message));
  }, []);
  useEffect(() => { loadScope(); }, [loadScope]);

  if (!scope) return <><h2>Admin Console</h2><Err msg={err} /></>;

  const facilities = scope.facilities;
  const hasSlots = isSuper || facilities.some((f) => f.type !== "PRINTING");
  const hasPrint = isSuper || facilities.some((f) => f.type === "PRINTING");

  const tabs = [
    hasSlots && ["bookings", "Bookings"],
    hasPrint && ["print", "Print Jobs"],
    hasSlots && ["services", "Resources"],
    isSuper && ["facilities", "Facilities"],
    isSuper && ["admins", "Admins"],
    isSuper && ["market", "Marketplace"],
    ["stats", "Analytics"],
  ].filter(Boolean);

  return (
    <>
      <h2>{isSuper ? "Super Admin Console" : "Admin Console"}</h2>
      {isSuper ? (
        <p className="muted">You can see and manage every facility.</p>
      ) : facilities.length ? (
        <p className="muted">You manage: <b>{facilities.map((f) => f.name).join(", ")}</b></p>
      ) : (
        <p className="error">No facility is assigned to you yet. Ask the super admin to give you access.</p>
      )}
      <Err msg={err} />
      <div className="tabs">
        {tabs.map(([k, text]) => (
          <button key={k} className={tab === k ? "on" : ""} onClick={() => setTab(k)}>{text}</button>
        ))}
      </div>

      {tab === "bookings" && <BookingsTab facilities={facilities.filter((f) => f.type !== "PRINTING")} />}
      {tab === "print" && <PrintTab facilities={facilities.filter((f) => f.type === "PRINTING")} />}
      {tab === "services" && <ServicesTab facilities={facilities.filter((f) => f.type !== "PRINTING")} />}
      {tab === "facilities" && <FacilitiesTab facilities={facilities} onChanged={loadScope} />}
      {tab === "admins" && <AdminsTab facilities={facilities} />}
      {tab === "market" && <MarketTab />}
      {tab === "stats" && <StatsTab isSuper={isSuper} />}
    </>
  );
}
