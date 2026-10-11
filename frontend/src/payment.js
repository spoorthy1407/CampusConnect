import { api } from "./api";

const loadRazorpay = () =>
  new Promise((resolve, reject) => {
    if (window.Razorpay) return resolve();
    const s = document.createElement("script");
    s.src = "https://checkout.razorpay.com/v1/checkout.js";
    s.onload = () => resolve();
    s.onerror = () => reject(new Error("Could not load Razorpay. Check your internet connection."));
    document.body.appendChild(s);
  });

export async function payForJob(jobId, user, existing) {
  const checkout = existing || (await api(`/api/print/jobs/${jobId}/pay`, { method: "POST" }));
  await loadRazorpay();
  return new Promise((resolve, reject) => {
    let paying = false;
    const rzp = new window.Razorpay({
      key: checkout.keyId,
      amount: checkout.amountPaise,
      currency: checkout.currency,
      order_id: checkout.orderId,
      name: "CampusConnect Printing",
      description: `Print job #${jobId}`,
      prefill: { name: user.fullName, email: user.email },
      theme: { color: "#2f5bea" },
      handler: async (resp) => {
        paying = true;
        try {
          await api(`/api/print/jobs/${jobId}/verify`, {
            method: "POST",
            body: {
              razorpayOrderId: resp.razorpay_order_id,
              razorpayPaymentId: resp.razorpay_payment_id,
              razorpaySignature: resp.razorpay_signature,
            },
          });
          resolve(true);
        } catch (e) {
          reject(e);
        }
      },
      modal: { ondismiss: () => { if (!paying) resolve(false); } },
    });
    rzp.open();
  });
}
