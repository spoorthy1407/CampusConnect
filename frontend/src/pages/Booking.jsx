import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { api } from "../api";
import { Err, FacilityInfo, TYPE_META, TYPE_ORDER, fmt, hhmm, todayStr } from "../ui";

export default function Booking() {
  const [params] = useSearchParams();
  const nav = useNavigate();
  const [facilities, setFacilities] = useState([]);
  const [cat, setCat] = useState("ALL");
  const [facilityId, setFacilityId] = useState(params.get("facility") || "");
  const [date, setDate] = useState(todayStr());
  const [start, setStart] = useState("");
  const [end, setEnd] = useState("");
  const [resources, setResources] = useState([]);
  const [sub, setSub] = useState("ALL");
  const [serviceId, setServiceId] = useState("");
  const [slots, setSlots] = useState([]);
  const [purpose, setPurpose] = useState("");
  const [err, setErr] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api("/api/facilities").then(setFacilities).catch((e) => setErr(e.message));
  }, []);

  const facility = facilities.find((f) => String(f.id) === String(facilityId));
  const validWindow = facilityId && date && start && end && end > start;

  useEffect(() => {
    setServiceId("");
    setSub("ALL");
    if (!validWindow || facility?.type === "PRINTING") { setResources([]); return; }
    setErr("");
    api(`/api/facilities/${facilityId}/availability?start=${date}T${start}&end=${date}T${end}`)
      .then(setResources)
      .catch((e) => setErr(e.message));
  }, [facilityId, date, start, end]);

  useEffect(() => {
    if (!serviceId) { setSlots([]); return; }
    api(`/api/services/${serviceId}/slots`).then(setSlots).catch(() => setSlots([]));
  }, [serviceId]);

  const subTypes = useMemo(() => [...new Set(resources.map((r) => r.subType).filter(Boolean))], [resources]);
  const visible = resources.filter((r) => sub === "ALL" || r.subType === sub);
  const shown = facilities.filter((f) => cat === "ALL" || f.type === cat);
  const cats = TYPE_ORDER.filter((t) => facilities.some((f) => f.type === t));
  const selected = resources.find((r) => String(r.id) === String(serviceId));

  const pickFacility = (id) => {
    setFacilityId(String(id));
    setErr("");
  };

  const submit = async (e) => {
    e.preventDefault();
    setErr("");
    if (!serviceId) return setErr("Choose a resource first");
    setBusy(true);
    try {
      await api("/api/bookings", {
        method: "POST",
        body: { serviceId: Number(serviceId), startTime: `${date}T${start}`, endTime: `${date}T${end}`, purpose },
      });
      nav("/profile", { state: { flash: "Request sent. You will get an email as soon as the admin approves it." } });
    } catch (ex) {
      setErr(ex.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <h2>Book a Service</h2>
      <div className="tabs">
        <button className={cat === "ALL" ? "on" : ""} onClick={() => setCat("ALL")}>All</button>
        {cats.map((t) => (
          <button key={t} className={cat === t ? "on" : ""} onClick={() => setCat(t)}>
            {TYPE_META[t].icon} {TYPE_META[t].label}
          </button>
        ))}
      </div>

      <div className="grid">
        {shown.map((f) => (
          <div key={f.id} className={`card pick ${String(f.id) === String(facilityId) ? "sel" : ""}`} onClick={() => pickFacility(f.id)}>
            <h3>{TYPE_META[f.type].icon} {f.name}</h3>
            <p className="muted">{f.blockName} · {hhmm(f.openTime)} – {hhmm(f.closeTime)}</p>
            {f.description && <p className="muted small-text">{f.description}</p>}
          </div>
        ))}
        {!shown.length && <p className="muted">No facilities in this category yet.</p>}
      </div>

      {facility && facility.type === "PRINTING" && (
        <div className="card">
          <FacilityInfo f={facility} />
          <p>Printing does not need a time slot. Upload your files and collect them from the counter.</p>
          <Link className="btn" to={`/print?facility=${facility.id}`}>Send files to {facility.name}</Link>
        </div>
      )}

      {facility && facility.type !== "PRINTING" && (
        <form className="card form wide" onSubmit={submit}>
          <h3>Book at {facility.name}</h3>
          <FacilityInfo f={facility} />
          <div className="row3">
            <label>Date
              <input type="date" min={todayStr()} value={date} onChange={(e) => setDate(e.target.value)} required />
            </label>
            <label>From
              <input type="time" step="900" value={start} onChange={(e) => setStart(e.target.value)} required />
            </label>
            <label>To
              <input type="time" step="900" value={end} onChange={(e) => setEnd(e.target.value)} required />
            </label>
          </div>

          {!validWindow && <p className="muted">Pick a date and a start and end time to see what is free.</p>}

          {validWindow && (
            <>
              {subTypes.length > 1 && (
                <div className="tabs">
                  <button type="button" className={sub === "ALL" ? "on" : ""} onClick={() => setSub("ALL")}>All</button>
                  {subTypes.map((s) => (
                    <button type="button" key={s} className={sub === s ? "on" : ""} onClick={() => setSub(s)}>{s}</button>
                  ))}
                </div>
              )}
              <div className="res-grid">
                {visible.map((r) => {
                  const state = r.status !== "AVAILABLE" ? "MAINTENANCE" : r.free ? "FREE" : "TAKEN";
                  return (
                    <button
                      type="button"
                      key={r.id}
                      disabled={state !== "FREE"}
                      className={`res ${state} ${String(r.id) === String(serviceId) ? "sel" : ""}`}
                      onClick={() => setServiceId(String(r.id))}
                    >
                      <b>{r.name}</b>
                      <small>{r.subType}{r.capacity > 1 ? ` · 👥 ${r.capacity}` : ""}</small>
                      <small>{state === "FREE" ? (r.pending ? `Free · ${r.pending} request(s) pending` : "Free") : state === "TAKEN" ? "Booked" : "Maintenance"}</small>
                    </button>
                  );
                })}
                {!visible.length && <p className="muted">No bookable resources here yet. Ask the admin to add some.</p>}
              </div>
            </>
          )}

          {selected && (
            <>
              <div className="note">
                <b>{selected.name}</b>{selected.location ? ` · ${selected.location}` : ""}
                <div>
                  <b>Already booked (approved):</b>
                  {slots.length ? slots.map((s, i) => <div key={i}>{fmt(s.startTime)} → {fmt(s.endTime)}</div>) : <div>No approved bookings yet.</div>}
                </div>
              </div>
              <label>Purpose
                <input value={purpose} onChange={(e) => setPurpose(e.target.value)} placeholder="e.g. Group study for DBMS exam" />
              </label>
            </>
          )}

          <Err msg={err} />
          <button className="btn" disabled={busy || !serviceId}>{busy ? "Sending…" : "Submit request"}</button>
        </form>
      )}
      {!facility && <Err msg={err} />}
    </>
  );
}
