import { useCallback, useEffect, useState } from "react";
import { api, download } from "../../api";
import { Badge, Err, fmt, label, money } from "../../ui";
import FacilityFilter from "./FacilityFilter";

const LIVE = ["AWAITING_PAYMENT", "QUEUED", "PRINTING", "READY"];

export default function PrintTab({ facilities }) {
  const [rows, setRows] = useState([]);
  const [fid, setFid] = useState("");
  const [err, setErr] = useState("");

  const load = useCallback(() => {
    api("/api/admin/print-jobs").then(setRows).catch((e) => setErr(e.message));
  }, []);
  useEffect(() => { load(); }, [load]);

  const act = async (path, body) => {
    setErr("");
    try { await api(path, { method: "PUT", body }); load(); } catch (e) { setErr(e.message); }
  };

  const cancel = (j) => {
    const reason = window.prompt("Reason for cancelling (optional, emailed to the student):");
    if (reason === null) return;
    act(`/api/admin/print-jobs/${j.id}/cancel`, { remarks: reason });
  };

  const getFile = async (j, f) => {
    setErr("");
    try { await download(`/api/admin/print-jobs/${j.id}/files/${f.id}`, f.originalName); } catch (e) { setErr(e.message); }
  };

  const list = rows.filter((j) => !fid || String(j.facility.id) === fid);
  const live = list.filter((j) => LIVE.includes(j.status)).sort((a, b) => a.id - b.id);
  const past = list.filter((j) => !LIVE.includes(j.status)).slice(0, 20);
  const base = (j) => `/api/admin/print-jobs/${j.id}`;

  return (
    <>
      <Err msg={err} />
      <FacilityFilter facilities={facilities} value={fid} onChange={setFid} />

      <h3>Print queue ({live.length})</h3>
      <table className="card">
        <thead><tr><th>Job</th><th>Student</th><th>Files</th><th>Settings</th><th>Payment</th><th>Status</th><th>Action</th></tr></thead>
        <tbody>
          {live.map((j) => (
            <tr key={j.id}>
              <td>#{j.id}<br /><small className="muted">{j.facility.name}</small><br /><small className="muted">{fmt(j.createdAt)}</small></td>
              <td>{j.user.fullName}<br /><small className="muted">{j.user.email}</small><br /><b className="code">{j.pickupCode}</b></td>
              <td>
                {j.files.map((f) => (
                  <div key={f.id}>
                    <button className="link" onClick={() => getFile(j, f)}>⬇ {f.originalName}</button> <small className="muted">({f.pages}p)</small>
                  </div>
                ))}
              </td>
              <td>
                {j.colorMode === "COLOR" ? "Colour" : "B&W"} · {j.paperSize} · {j.orientation === "LANDSCAPE" ? "Landscape" : "Portrait"}<br />
                {j.sides === "DOUBLE" ? "Double sided" : "Single sided"} · {j.copies} copies · {j.totalPages} pages
                {j.notes && <><br /><small className="muted">Note: {j.notes}</small></>}
              </td>
              <td>{money(j.amount)}<br /><small className="muted">{label(j.paymentMode)}</small><br /><Badge s={j.paymentStatus} /></td>
              <td><Badge s={j.status} /></td>
              <td className="actions">
                {j.status === "AWAITING_PAYMENT" && <small className="muted">Waiting for payment</small>}
                {j.status === "QUEUED" && <button className="btn small" onClick={() => act(`${base(j)}/start`)}>Start printing</button>}
                {["QUEUED", "PRINTING"].includes(j.status) && <button className="btn small" onClick={() => act(`${base(j)}/ready`)}>Mark ready</button>}
                {j.status === "READY" && <button className="btn small" onClick={() => act(`${base(j)}/collected`)}>Collected</button>}
                <button className="btn small ghost" onClick={() => cancel(j)}>Cancel</button>
              </td>
            </tr>
          ))}
          {!live.length && <tr><td colSpan="7" className="muted">No jobs waiting 🎉</td></tr>}
        </tbody>
      </table>

      <h3>Recent jobs</h3>
      <table className="card">
        <tbody>
          {past.map((j) => (
            <tr key={j.id}>
              <td>#{j.id}</td>
              <td>{j.user.fullName}</td>
              <td>{j.totalPages}p × {j.copies}</td>
              <td>{money(j.amount)} <Badge s={j.paymentStatus} /></td>
              <td><Badge s={j.status} /></td>
              <td>{j.paymentStatus === "REFUND_DUE" && <button className="btn small" onClick={() => act(`${base(j)}/refunded`)}>Mark refunded</button>}</td>
            </tr>
          ))}
          {!past.length && <tr><td className="muted">Nothing yet.</td></tr>}
        </tbody>
      </table>
    </>
  );
}
