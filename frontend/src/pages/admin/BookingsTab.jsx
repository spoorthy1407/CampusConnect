import { useCallback, useEffect, useState } from "react";
import { api } from "../../api";
import { Badge, Err, fmt } from "../../ui";
import FacilityFilter from "./FacilityFilter";

export default function BookingsTab({ facilities }) {
  const [rows, setRows] = useState([]);
  const [fid, setFid] = useState("");
  const [err, setErr] = useState("");

  const load = useCallback(() => {
    api("/api/admin/bookings").then(setRows).catch((e) => setErr(e.message));
  }, []);
  useEffect(() => { load(); }, [load]);

  const act = async (path, body) => {
    setErr("");
    try { await api(path, { method: "PUT", body }); load(); } catch (e) { setErr(e.message); }
  };

  const reject = (b) => {
    const reason = window.prompt("Reason for rejecting (optional, emailed to the student):");
    if (reason === null) return;
    act(`/api/admin/bookings/${b.id}/reject`, { remarks: reason });
  };

  const list = rows.filter((b) => !fid || String(b.service.facility.id) === fid);
  const pending = list.filter((b) => b.status === "PENDING");
  const approved = list.filter((b) => b.status === "APPROVED");
  const history = list.filter((b) => !["PENDING", "APPROVED"].includes(b.status)).slice(0, 15);

  const Who = ({ b }) => <td>{b.user.fullName}<br /><small className="muted">{b.user.email}</small></td>;
  const What = ({ b }) => <td>{b.service.name}<br /><small className="muted">{b.service.facility.name}</small></td>;

  return (
    <>
      <Err msg={err} />
      <FacilityFilter facilities={facilities} value={fid} onChange={setFid} />

      <h3>Pending requests ({pending.length})</h3>
      <table className="card">
        <thead><tr><th>Student</th><th>Resource</th><th>When</th><th>Purpose</th><th>Action</th></tr></thead>
        <tbody>
          {pending.map((b) => (
            <tr key={b.id}>
              <Who b={b} /><What b={b} />
              <td>{fmt(b.startTime)} → {fmt(b.endTime)}</td>
              <td>{b.purpose}</td>
              <td>
                <button className="btn small" onClick={() => act(`/api/admin/bookings/${b.id}/approve`)}>Approve</button>{" "}
                <button className="btn small ghost" onClick={() => reject(b)}>Reject</button>
              </td>
            </tr>
          ))}
          {!pending.length && <tr><td colSpan="5" className="muted">No pending requests 🎉</td></tr>}
        </tbody>
      </table>

      <h3>Approved (mark complete after use)</h3>
      <table className="card">
        <tbody>
          {approved.map((b) => (
            <tr key={b.id}>
              <Who b={b} /><What b={b} />
              <td>{fmt(b.startTime)} → {fmt(b.endTime)}</td>
              <td><button className="btn small" onClick={() => act(`/api/admin/bookings/${b.id}/complete`)}>Complete</button></td>
            </tr>
          ))}
          {!approved.length && <tr><td className="muted">None.</td></tr>}
        </tbody>
      </table>

      <h3>Recent history</h3>
      <table className="card">
        <tbody>
          {history.map((b) => (
            <tr key={b.id}>
              <Who b={b} /><What b={b} />
              <td>{fmt(b.startTime)}</td>
              <td><Badge s={b.status} /></td>
            </tr>
          ))}
          {!history.length && <tr><td className="muted">Nothing yet.</td></tr>}
        </tbody>
      </table>
    </>
  );
}
