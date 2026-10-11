import { useEffect, useState } from "react";
import { api } from "../../api";
import { Err, label, money } from "../../ui";

function Bars({ rows }) {
  const max = Math.max(1, ...rows.map((r) => r.count));
  return rows.length ? rows.map((r) => (
    <div className="bar" key={r.name}>
      <span title={r.name}>{label(r.name)}</span>
      <div><i style={{ width: `${(r.count / max) * 100}%` }} /></div>
      <b>{r.count}</b>
    </div>
  )) : <p className="muted">No data yet.</p>;
}

export default function StatsTab({ isSuper }) {
  const [s, setS] = useState(null);
  const [err, setErr] = useState("");

  useEffect(() => {
    api("/api/admin/analytics").then(setS).catch((e) => setErr(e.message));
  }, []);

  if (!s) return <Err msg={err} />;

  return (
    <div className="grid">
      <div className="card"><h3>Bookings per facility</h3><Bars rows={s.bookingsPerFacility} /></div>
      <div className="card"><h3>Busiest resources</h3><Bars rows={s.bookingsPerResource} /></div>
      <div className="card"><h3>Most active students</h3><Bars rows={s.topStudents} /></div>
      <div className="card"><h3>Bookings by status</h3><Bars rows={s.bookingsByStatus} /></div>
      <div className="card"><h3>Printing</h3>
        <p>Print jobs: <b>{s.printJobs}</b></p>
        <p>Pages printed: <b>{s.pagesPrinted}</b></p>
        <p>Paid revenue: <b>{money(s.printRevenue)}</b></p>
        <Bars rows={s.printJobsByStatus} />
      </div>
      {isSuper && (
        <div className="card"><h3>Marketplace</h3>
          <p>Active listings: <b>{s.activeListings}</b></p>
          <p>Sold items: <b>{s.soldListings}</b></p>
          <p>Registered users: <b>{s.totalUsers}</b></p>
        </div>
      )}
    </div>
  );
}
