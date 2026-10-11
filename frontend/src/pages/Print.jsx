import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { api } from "../api";
import { useAuth } from "../AuthContext";
import { payForJob } from "../payment";
import { Err, FacilityInfo, money, sizeLabel } from "../ui";

const ACCEPT = ".pdf,.jpg,.jpeg,.png,.doc,.docx,.ppt,.pptx";
const AUTO_COUNTED = ["pdf", "jpg", "jpeg", "png"];
const extOf = (name) => name.split(".").pop().toLowerCase();

function Seg({ label, value, onChange, options }) {
  return (
    <div className="seg-wrap">
      <span className="seg-label">{label}</span>
      <div className="seg">
        {options.map(([v, text, disabled]) => (
          <button type="button" key={v} disabled={disabled} className={value === v ? "on" : ""} onClick={() => onChange(v)}>
            {text}
          </button>
        ))}
      </div>
    </div>
  );
}

export default function Print() {
  const { user } = useAuth();
  const nav = useNavigate();
  const [params] = useSearchParams();
  const [config, setConfig] = useState(null);
  const [facilityId, setFacilityId] = useState(params.get("facility") || "");
  const [items, setItems] = useState([]);
  const [o, setO] = useState({
    colorMode: "BLACK_WHITE",
    orientation: "PORTRAIT",
    paperSize: "A4",
    sides: "SINGLE",
    copies: 1,
    paymentMode: "COUNTER",
    notes: "",
  });
  const [err, setErr] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api("/api/print/config")
      .then((c) => {
        setConfig(c);
        if (!facilityId && c.facilities.length === 1) setFacilityId(String(c.facilities[0].id));
        if (c.razorpayEnabled) setO((x) => ({ ...x, paymentMode: "ONLINE" }));
      })
      .catch((e) => setErr(e.message));
  }, []);

  if (!config) return <><h2>Print</h2><Err msg={err} /></>;

  const facility = config.facilities.find((f) => String(f.id) === String(facilityId));
  const set = (k, v) => setO((x) => ({ ...x, [k]: v }));
  const rate = (o.colorMode === "COLOR" ? config.priceColor : config.priceBw) * (o.paperSize === "A3" ? config.a3Multiplier : 1);

  const addFiles = (e) => {
    const added = [...e.target.files].map((file) => ({ file, pages: "" }));
    setItems((prev) => [...prev, ...added].slice(0, config.maxFiles));
    e.target.value = "";
  };
  const removeItem = (i) => setItems((prev) => prev.filter((_, idx) => idx !== i));
  const setPages = (i, v) => setItems((prev) => prev.map((it, idx) => (idx === i ? { ...it, pages: v } : it)));

  const submit = async (e) => {
    e.preventDefault();
    setErr("");
    if (!facility) return setErr("Choose a printing centre");
    if (!items.length) return setErr("Attach at least one file");
    const copies = Number(o.copies);
    if (!Number.isInteger(copies) || copies < 1 || copies > 50) return setErr("Copies must be between 1 and 50");
    for (const it of items) {
      if (!AUTO_COUNTED.includes(extOf(it.file.name)) && !(Number(it.pages) >= 1)) {
        return setErr(`Enter the number of pages for ${it.file.name}`);
      }
    }
    const fd = new FormData();
    fd.append("facilityId", facility.id);
    Object.entries({ ...o, copies }).forEach(([k, v]) => fd.append(k, v));
    items.forEach((it) => {
      fd.append("files", it.file);
      fd.append("pages", AUTO_COUNTED.includes(extOf(it.file.name)) ? "0" : String(Number(it.pages)));
    });
    setBusy(true);
    try {
      const r = await api("/api/print/jobs", { method: "POST", form: fd });
      let flash = `Print job #${r.job.id} sent. Total ${money(r.job.amount)}. Pickup code: ${r.job.pickupCode}.`;
      if (r.checkout) {
        try {
          const paid = await payForJob(r.job.id, user, r.checkout);
          flash = paid
            ? `Payment received for job #${r.job.id}. Pickup code: ${r.job.pickupCode}. You will get an email when it is ready.`
            : `Job #${r.job.id} is saved but not paid yet. Press Pay now in My Print Jobs to finish.`;
        } catch (ex) {
          flash = `Job #${r.job.id} is saved but payment did not complete: ${ex.message}`;
        }
      } else if (o.paymentMode === "ONLINE") {
        flash = `Job #${r.job.id} is saved but the payment could not be started. Press Pay now in My Print Jobs.`;
      } else {
        flash += " Pay at the counter when you collect.";
      }
      nav("/profile", { state: { flash } });
    } catch (ex) {
      setErr(ex.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <h2>Print your documents</h2>
      <p className="muted">Send files straight to the printing centre, choose how you want them printed, and collect them without waiting in a queue.</p>
      {!config.facilities.length && <p className="error">No printing centre is open for online jobs yet.</p>}

      <form className="card form wide" onSubmit={submit}>
        <label>Printing centre
          <select value={facilityId} onChange={(e) => setFacilityId(e.target.value)} required>
            <option value="">-- choose --</option>
            {config.facilities.map((f) => <option key={f.id} value={f.id}>{f.name} ({f.blockName})</option>)}
          </select>
        </label>
        {facility && <FacilityInfo f={facility} />}

        <label>Files (PDF, JPG, PNG, DOC, DOCX, PPT, PPTX · up to {config.maxFiles} files · 15 MB each)
          <input type="file" accept={ACCEPT} multiple onChange={addFiles} />
        </label>
        {items.length > 0 && (
          <div className="files">
            {items.map((it, i) => {
              const auto = AUTO_COUNTED.includes(extOf(it.file.name));
              return (
                <div className="file-row" key={i}>
                  <span>📄 {it.file.name} <small className="muted">({sizeLabel(it.file.size)})</small></span>
                  {auto ? (
                    <small className="muted">pages counted automatically</small>
                  ) : (
                    <input className="pages-input" type="number" min="1" max="500" placeholder="Pages" value={it.pages} onChange={(e) => setPages(i, e.target.value)} />
                  )}
                  <button type="button" className="btn small ghost" onClick={() => removeItem(i)}>Remove</button>
                </div>
              );
            })}
          </div>
        )}

        <Seg label="Colour" value={o.colorMode} onChange={(v) => set("colorMode", v)}
          options={[["BLACK_WHITE", `Black & white · ${money(config.priceBw)}/page`], ["COLOR", `Colour · ${money(config.priceColor)}/page`]]} />
        <Seg label="Orientation" value={o.orientation} onChange={(v) => set("orientation", v)}
          options={[["PORTRAIT", "Vertical (portrait)"], ["LANDSCAPE", "Horizontal (landscape)"]]} />
        <Seg label="Paper size" value={o.paperSize} onChange={(v) => set("paperSize", v)}
          options={[["A4", "A4"], ["A3", `A3 · ×${config.a3Multiplier} price`]]} />
        <Seg label="Sides" value={o.sides} onChange={(v) => set("sides", v)}
          options={[["SINGLE", "Single sided"], ["DOUBLE", "Double sided"]]} />

        <label>Copies
          <input type="number" min="1" max="50" value={o.copies} onChange={(e) => set("copies", e.target.value)} required />
        </label>
        <label>Note for the printing centre (optional)
          <input value={o.notes} maxLength={400} onChange={(e) => set("notes", e.target.value)} placeholder="e.g. Staple the pages, pages 3-10 only" />
        </label>

        <Seg label="Payment" value={o.paymentMode} onChange={(v) => set("paymentMode", v)}
          options={[["ONLINE", "Pay online (Razorpay)", !config.razorpayEnabled], ["COUNTER", "Pay at counter"]]} />
        {!config.razorpayEnabled && <small className="muted">Online payment is switched off until Razorpay keys are added on the server.</small>}

        <div className="note">
          Rate: <b>{money(rate)}</b> per page{o.sides === "DOUBLE" ? " (each side counts as a page)" : ""}. The final amount is worked out from the page count after upload.
        </div>

        <Err msg={err} />
        <button className="btn" disabled={busy || !config.facilities.length}>
          {busy ? "Uploading…" : o.paymentMode === "ONLINE" ? "Upload & pay" : "Send to printer"}
        </button>
      </form>
    </>
  );
}
