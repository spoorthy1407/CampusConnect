import { useCallback, useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
import { api } from "../api";
import { useAuth } from "../AuthContext";
import { payForJob } from "../payment";
import { Badge, Err, Flash, fmt, label, money } from "../ui";

export default function Profile() {
  const { user } = useAuth();
  const location = useLocation();
  const [bookings, setBookings] = useState([]);
  const [prints, setPrints] = useState([]);
  const [listings, setListings] = useState([]);
  const [flash, setFlash] = useState(location.state?.flash || "");
  const [err, setErr] = useState("");

  const load = useCallback(() => {
    Promise.all([api("/api/bookings/my"), api("/api/print/jobs/my"), api("/api/listings/my")])
      .then(([b, p, l]) => { setBookings(b); setPrints(p); setListings(l); })
      .catch((e) => setErr(e.message));
  }, []);
  useEffect(() => { load(); }, [load]);

  const act = async (path, method = "PUT") => {
    setErr("");
    setFlash("");
    try { await api(path, { method }); load(); } catch (e) { setErr(e.message); }
  };

  const pay = async (job) => {
    setErr("");
    setFlash("");
    try {
      const paid = await payForJob(job.id, user);
      if (paid) setFlash(`Payment received for job #${job.id}.`);
      load();
    } catch (e) {
      setErr(e.message);
      load();
    }
  };

  const options = (j) =>
    [j.colorMode === "COLOR" ? "Colour" : "B&W", j.paperSize, j.orientation === "LANDSCAPE" ? "Landscape" : "Portrait",
      j.sides === "DOUBLE" ? "Double sided" : "Single sided"].join(" · ");

  return (
    <>
      <Flash msg={flash} />
      <Err msg={err} />

      <h2>My Bookings</h2>
      <table className="card">
        <thead><tr><th>Resource</th><th>When</th><th>Purpose</th><th>Status</th><th></th></tr></thead>
        <tbody>
          {bookings.map((b) => (
            <tr key={b.id}>
              <td>{b.service.name}<br /><small className="muted">{b.service.facility.name}</small></td>
              <td>{fmt(b.startTime)} → {fmt(b.endTime)}</td>
              <td>{b.purpose}{b.remarks && <><br /><small className="muted">Admin note: {b.remarks}</small></>}</td>
              <td><Badge s={b.status} /></td>
              <td>{["PENDING", "APPROVED"].includes(b.status) && <button className="btn small ghost" onClick={() => act(`/api/bookings/${b.id}/cancel`)}>Cancel</button>}</td>
            </tr>
          ))}
          {!bookings.length && <tr><td colSpan="5" className="muted">No bookings yet.</td></tr>}
        </tbody>
      </table>

      <h2>My Print Jobs</h2>
      <table className="card">
        <thead><tr><th>Job</th><th>Details</th><th>Amount</th><th>Status</th><th>Pickup code</th><th></th></tr></thead>
        <tbody>
          {prints.map((j) => (
            <tr key={j.id}>
              <td>#{j.id}<br /><small className="muted">{j.facility.name}</small><br /><small className="muted">{fmt(j.createdAt)}</small></td>
              <td>
                {j.files.map((f) => <div key={f.id}>📄 {f.originalName} <small className="muted">({f.pages}p)</small></div>)}
                <small className="muted">{options(j)} · {j.copies} copies · {j.totalPages} pages</small>
                {j.remarks && <><br /><small className="muted">Note: {j.remarks}</small></>}
              </td>
              <td>{money(j.amount)}<br /><small className="muted">{label(j.paymentMode)} · {label(j.paymentStatus)}</small></td>
              <td><Badge s={j.status} /></td>
              <td>{j.status !== "CANCELLED" ? <b className="code">{j.pickupCode}</b> : "-"}</td>
              <td>
                {j.status === "AWAITING_PAYMENT" && <button className="btn small" onClick={() => pay(j)}>Pay now</button>}{" "}
                {["AWAITING_PAYMENT", "QUEUED"].includes(j.status) && <button className="btn small ghost" onClick={() => act(`/api/print/jobs/${j.id}/cancel`)}>Cancel</button>}
              </td>
            </tr>
          ))}
          {!prints.length && <tr><td colSpan="6" className="muted">You have not sent anything to print.</td></tr>}
        </tbody>
      </table>

      <h2>My Listings</h2>
      <table className="card">
        <thead><tr><th>Title</th><th>Category</th><th>Price</th><th>Status</th><th></th></tr></thead>
        <tbody>
          {listings.map((l) => (
            <tr key={l.id}>
              <td>{l.title}</td><td>{l.category}</td><td>{money(l.price)}</td><td><Badge s={l.status} /></td>
              <td>
                {l.status === "ACTIVE" && <button className="btn small" onClick={() => act(`/api/listings/${l.id}/sold`)}>Mark sold</button>}{" "}
                <button className="btn small ghost" onClick={() => act(`/api/listings/${l.id}`, "DELETE")}>Delete</button>
              </td>
            </tr>
          ))}
          {!listings.length && <tr><td colSpan="5" className="muted">You have not listed anything.</td></tr>}
        </tbody>
      </table>
    </>
  );
}
