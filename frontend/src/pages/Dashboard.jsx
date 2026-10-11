import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api";
import { useAuth } from "../AuthContext";
import { Err, TYPE_META, TYPE_ORDER, hhmm, money, imgUrl } from "../ui";

export default function Dashboard() {
  const { user } = useAuth();
  const [tab, setTab] = useState("services");
  const [facilities, setFacilities] = useState([]);
  const [services, setServices] = useState([]);
  const [listings, setListings] = useState([]);
  const [mine, setMine] = useState([]);
  const [err, setErr] = useState("");

  useEffect(() => {
    Promise.all([api("/api/facilities"), api("/api/services"), api("/api/listings"), api("/api/bookings/my")])
      .then(([f, s, l, m]) => { setFacilities(f); setServices(s); setListings(l); setMine(m); })
      .catch((e) => setErr(e.message));
  }, []);

  const pending = mine.filter((b) => b.status === "PENDING").length;
  const approved = mine.filter((b) => b.status === "APPROVED" && new Date(b.endTime) > new Date()).length;
  const countFor = (f) => services.filter((s) => s.facility.id === f.id && s.status === "AVAILABLE").length;

  return (
    <>
      <h2>Welcome, {user.fullName} 👋</h2>
      <Err msg={err} />
      <div className="stats">
        <div className="card stat"><b>{facilities.length}</b><span>Campus facilities</span></div>
        <div className="card stat"><b>{pending}</b><span>Pending requests</span></div>
        <div className="card stat"><b>{approved}</b><span>Upcoming approved</span></div>
        <div className="card stat"><b>{listings.length}</b><span>Active listings</span></div>
      </div>

      <div className="tabs">
        <button className={tab === "services" ? "on" : ""} onClick={() => setTab("services")}>Services</button>
        <button className={tab === "market" ? "on" : ""} onClick={() => setTab("market")}>Marketplace</button>
      </div>

      {tab === "services" ? (
        TYPE_ORDER.map((t) => {
          const list = facilities.filter((f) => f.type === t);
          if (!list.length) return null;
          return (
            <section key={t}>
              <h3>{TYPE_META[t].icon} {TYPE_META[t].label}</h3>
              <div className="grid">
                {list.map((f) => (
                  <div className="card" key={f.id}>
                    <h3>{f.name}</h3>
                    <p className="muted">{f.blockName} · {hhmm(f.openTime)} – {hhmm(f.closeTime)}</p>
                    {f.type !== "PRINTING" && <p className="muted">{countFor(f)} bookable resources</p>}
                    {f.type === "PRINTING"
                      ? <Link className="btn small" to={`/print?facility=${f.id}`}>Send files to print</Link>
                      : <Link className="btn small" to={`/book?facility=${f.id}`}>Book now</Link>}
                  </div>
                ))}
              </div>
            </section>
          );
        })
      ) : (
        <>
          <div className="grid">
            {listings.slice(0, 6).map((l) => (
              <div className="card" key={l.id}>
                {l.imageUrl && <img className="thumb" src={imgUrl(l.imageUrl)} alt={l.title} />}
                <h3>{l.title}</h3>
                <p className="muted">{l.category} · by {l.seller.fullName}</p>
                <b>{money(l.price)}</b>
              </div>
            ))}
            {!listings.length && <p className="muted">No active listings yet.</p>}
          </div>
          <p><Link to="/marketplace">See all listings →</Link></p>
        </>
      )}
    </>
  );
}
