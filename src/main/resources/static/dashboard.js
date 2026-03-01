/* ===============================
   COMMON CHART CONFIG
================================= */

const chartAreaBackground = {
  id: "chartAreaBackground",
  beforeDraw(chart) {
    const { ctx, chartArea } = chart;
    if (!chartArea) return;

    ctx.save();
    ctx.fillStyle = "#1f2937";
    ctx.fillRect(
      chartArea.left,
      chartArea.top,
      chartArea.right - chartArea.left,
      chartArea.bottom - chartArea.top
    );
    ctx.restore();
  }
};

const commonChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  interaction: {
    mode: "index",
    intersect: false
  },
  plugins: {
    legend: {
      labels: {
        color: "white",
        font: { size: 14 }
      }
    }
  },
  scales: {
    x: {
      ticks: { color: "white", font: { size: 12 } },
      grid: { color: "#374151" }
    },
    y: {
      ticks: { color: "white", font: { size: 12 } },
      grid: { color: "#374151" }
    }
  }
};

/* ===============================
   CHART INSTANCES
================================= */

let fraudChartInstance = null;
let suspiciousChartInstance = null;
let activityChartInstance = null;

/* ===============================
   DASHBOARD LOADER
================================= */

async function loadDashboard() {
  try {
    const res = await fetch("/api/fraud/metrics");
    const data = await res.json();

    updateCards(data);
    drawFraudChart(data.topFraudIPs || []);
    drawSuspiciousChart(data.topSuspiciousIPs || []);
    drawActivityChart(data.activity || []);
    updateTable(data.alerts || []);
  } catch (e) {
    console.error("Dashboard load failed", e);
  }
}

/* ===============================
   CARDS
================================= */

function updateCards(data) {
  document.getElementById("totalLogs").innerText =
    "Total Logs: " + (data.totalLogs || 0);

  document.getElementById("fraudCount").innerText =
    "Fraud Count: " + (data.fraudCount || 0);

  document.getElementById("suspiciousCount").innerText =
    "Suspicious Count: " + (data.suspiciousCount || 0);

  const rate = data.fraudRate ? data.fraudRate.toFixed(2) : "0.00";

  document.getElementById("fraudRate").innerText =
    "Fraud Rate: " + rate + "%";
}

/* ===============================
   GENERIC BAR CHART BUILDER
================================= */

function buildBarChart(ctx, label, labels, values, color, existingChart) {
  if (!existingChart) {
    return new Chart(ctx, {
      type: "bar",
      data: {
        labels: labels,
        datasets: [{
          label: label,
          data: values,
          backgroundColor: color,
          borderRadius: 6,
          barThickness: 30
        }]
      },
      options: commonChartOptions,
      plugins: [chartAreaBackground]
    });
  }

  existingChart.data.labels = labels;
  existingChart.data.datasets[0].data = values;
  existingChart.update();

  return existingChart;
}

/* ===============================
   FRAUD CHART
================================= */

function drawFraudChart(fraudData) {
  const ctx = document.getElementById("fraudChart");

  const labels = fraudData.map(i => i.ip);
  const values = fraudData.map(i => i.count);

  fraudChartInstance = buildBarChart(
    ctx,
    "Fraud IPs",
    labels,
    values,
    "#ef4444",
    fraudChartInstance
  );
}

/* ===============================
   SUSPICIOUS CHART
================================= */

function drawSuspiciousChart(suspiciousData) {
  const ctx = document.getElementById("suspiciousChart");

  const labels = suspiciousData.map(i => i.ip);
  const values = suspiciousData.map(i => i.count);

  suspiciousChartInstance = buildBarChart(
    ctx,
    "Suspicious IPs",
    labels,
    values,
    "#f59e0b",
    suspiciousChartInstance
  );
}

/* ===============================
   ACTIVITY LINE CHART
================================= */

function drawActivityChart(activityData) {
  const ctx = document.getElementById("activityChart");

  const labels = activityData.map(i => i.time);
  const values = activityData.map(i => i.count);

  if (!activityChartInstance) {
    activityChartInstance = new Chart(ctx, {
      type: "line",
      data: {
        labels: labels,
        datasets: [{
          label: "Logs processed",
          data: values,
          borderColor: "#22c55e",
          backgroundColor: "#22c55e",
          tension: 0.3,
          fill: false,
          pointRadius: 2
        }]
      },
      options: commonChartOptions,
      plugins: [chartAreaBackground]
    });
  } else {
    activityChartInstance.data.labels = labels;
    activityChartInstance.data.datasets[0].data = values;
    activityChartInstance.update();
  }
}

/* ===============================
   ALERT TABLE (SCROLL SAFE)
================================= */

function updateTable(alerts) {
  const tbody = document.querySelector("#alertsTable tbody");

  const scrollTop =
    document.documentElement.scrollTop || document.body.scrollTop;

  tbody.innerHTML = "";

  alerts.forEach(a => {
    const row = document.createElement("tr");

    row.innerHTML = `
      <td>${a.time}</td>
      <td>${a.type}</td>
      <td>${a.ip}</td>
      <td>${a.details}</td>
    `;

    tbody.appendChild(row);
  });

  window.scrollTo({ top: scrollTop });
}

/* ===============================
   AUTO REFRESH
================================= */

setInterval(loadDashboard, 5000);
loadDashboard();