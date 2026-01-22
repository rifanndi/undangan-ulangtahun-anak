<!DOCTYPE html>
<html lang="id">
  <head>
    <meta charset="UTF-8" />
    <meta
      name="viewport"
      content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"
    />
    <title>Dashboard Omset Responsif</title>
    <style>
      body {
        margin: 0;
        padding: 0;
        overflow: hidden;
        font-family: "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
        background-color: #f4f6f8;
      }

      /* --- UI MENGAMBANG (RESPONSIF) --- */
      .overlay-ui {
        position: absolute;
        top: 20px;
        left: 30px;
        background: rgba(255, 255, 255, 0.9);
        padding: 20px;
        border-radius: 16px;
        box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
        backdrop-filter: blur(8px);
        z-index: 10;
        border: 1px solid rgba(255, 255, 255, 0.8);
        display: flex;
        gap: 25px;
        align-items: center;
        max-width: 600px;
        transition: all 0.3s ease;
      }

      .text-section {
        flex: 1.2;
        border-right: 1px solid #eee;
        padding-right: 20px;
      }

      h1 {
        margin: 0 0 5px 0;
        font-size: 20px;
        color: #2c3e50;
        font-weight: 700;
      }
      p {
        margin: 0 0 10px 0;
        color: #7f8c8d;
        font-size: 13px;
      }

      .total-omset {
        font-size: 26px;
        font-weight: bold;
        color: #27ae60;
        margin-bottom: 5px;
        display: block;
      }

      .loop-indicator {
        height: 4px;
        background: #eee;
        border-radius: 2px;
        overflow: hidden;
        width: 100%;
        margin-top: 10px;
      }
      .loop-bar {
        height: 100%;
        background: #27ae60;
        width: 0%;
      }

      .pie-section {
        display: flex;
        flex-direction: column;
        align-items: center;
        width: 140px;
      }

      /* Canvas Pie Kecil */
      #pieCanvas {
        width: 120px;
        height: 120px;
      }

      .legend {
        margin-top: 8px;
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 4px;
        font-size: 10px;
        width: 100%;
      }
      .legend-item {
        display: flex;
        align-items: center;
        color: #555;
        white-space: nowrap;
      }
      .dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;
        margin-right: 4px;
        flex-shrink: 0;
      }

      canvas#fullscreenChart {
        display: block;
        width: 100vw;
        height: 100vh;
      }

      /* --- MEDIA QUERY UNTUK HP (Mobile) --- */
      @media (max-width: 768px) {
        .overlay-ui {
          top: 10px;
          left: 50%;
          transform: translateX(-50%); /* Posisi Tengah */
          flex-direction: column; /* Susun ke bawah */
          width: 85%; /* Lebar hampir penuh */
          padding: 15px;
          gap: 10px;
        }

        .text-section {
          border-right: none; /* Hapus garis pemisah */
          border-bottom: 1px solid #eee; /* Ganti jadi garis bawah */
          padding-right: 0;
          padding-bottom: 10px;
          text-align: center; /* Teks Rata Tengah */
          width: 100%;
        }

        h1 {
          font-size: 18px;
        }
        .total-omset {
          font-size: 22px;
        }

        .pie-section {
          flex-direction: row; /* Pie dan Legend sebelahan di HP biar hemat tempat vertikal */
          width: 100%;
          justify-content: center;
          gap: 15px;
        }

        #pieCanvas {
          width: 80px;
          height: 80px;
        } /* Pie lebih kecil di HP */

        .legend {
          display: flex;
          flex-direction: column;
          width: auto;
        }
      }
    </style>
  </head>
  <body>
    <div class="overlay-ui">
      <div class="text-section">
        <h1>Laporan Omset</h1>
        <p>Jan 2024 - Des 2025</p>
        <span class="total-omset" id="displayOmset">Rp 0 M</span>
        <div class="loop-indicator">
          <div class="loop-bar" id="progressBar"></div>
        </div>
      </div>

      <div class="pie-section">
        <canvas id="pieCanvas" width="240" height="240"></canvas>
        <div class="legend">
          <div class="legend-item">
            <div class="dot" style="background: #3498db"></div>
            Meta (92%)
          </div>
          <div class="legend-item">
            <div class="dot" style="background: #2ecc71"></div>
            Affil (5%)
          </div>
          <div class="legend-item">
            <div class="dot" style="background: #95a5a6"></div>
            Unkn (2%)
          </div>
          <div class="legend-item">
            <div class="dot" style="background: #e74c3c"></div>
            Offline (1%)
          </div>
        </div>
      </div>
    </div>

    <canvas id="fullscreenChart"></canvas>

    <script>
      // --- KONFIGURASI UMUM ---
      let progress = 0;
      let isPaused = false;

      // --- DATA GENERATOR (Jan 24 - Des 25) ---
      const labels = [];
      const dataValues = [];
      const monthNames = [
        "Jan",
        "Feb",
        "Mar",
        "Apr",
        "Mei",
        "Jun",
        "Jul",
        "Agu",
        "Sep",
        "Okt",
        "Nov",
        "Des",
      ];
      let valueTracker = 1.2;

      for (let year = 2024; year <= 2025; year++) {
        for (let m = 0; m < 12; m++) {
          labels.push(`${monthNames[m]} '${year.toString().substr(2)}`); // Format Jan '24
          if (year === 2024) {
            valueTracker += (Math.random() - 0.3) * 0.5;
            if (valueTracker < 1) valueTracker = 1.2;
          } else {
            valueTracker = valueTracker * 1.15; // Eksponensial
          }
          dataValues.push(valueTracker);
        }
      }
      const maxVal = Math.max(...dataValues);
      const totalPoints = dataValues.length - 1;

      // --- SETUP CANVAS ---
      const lineCanvas = document.getElementById("fullscreenChart");
      const lineCtx = lineCanvas.getContext("2d");
      const displayOmset = document.getElementById("displayOmset");

      const pieCanvas = document.getElementById("pieCanvas");
      const pieCtx = pieCanvas.getContext("2d");
      const pieData = [
        { v: 92, c: "#3498db" },
        { v: 5, c: "#2ecc71" },
        { v: 2, c: "#95a5a6" },
        { v: 1, c: "#e74c3c" },
      ];

      // --- VARIABEL RESPONSIVE ---
      let width, height;
      let paddingLeft, paddingBottom, paddingTop;
      let isMobile = false;

      function resizeCanvas() {
        lineCanvas.width = window.innerWidth;
        lineCanvas.height = window.innerHeight;
        width = lineCanvas.width;
        height = lineCanvas.height;

        // Deteksi Mobile (< 768px)
        isMobile = width < 768;

        // Atur Padding Berdasarkan Ukuran Layar
        if (isMobile) {
          paddingLeft = 55; // Lebih sempit di HP
          paddingBottom = 50;
          paddingTop = 260; // Turunkan grafik agar tidak ketutup UI Card yang numpuk
        } else {
          paddingLeft = 80;
          paddingBottom = 60;
          paddingTop = 80;
        }
      }

      window.addEventListener("resize", resizeCanvas);
      resizeCanvas();

      // --- HELPER FUNCTIONS ---
      function getX(index) {
        // Lebar efektif grafik
        const chartW = width - paddingLeft - (isMobile ? 10 : 30);
        return paddingLeft + index * (chartW / totalPoints);
      }

      function getY(value) {
        const chartH = height - paddingBottom - paddingTop;
        return height - paddingBottom - (value / (maxVal * 1.1)) * chartH;
      }

      function formatRupiah(val) {
        return "Rp " + val.toFixed(1) + " M";
      }

      // --- ANIMATION LOOP ---
      function animate() {
        if (isPaused) return;

        // Update UI Bar
        document.getElementById("progressBar").style.width = progress + "%";

        // --- A. LINE CHART ---
        lineCtx.clearRect(0, 0, width, height);

        // 1. Grid & Y-Axis Labels
        lineCtx.beginPath();
        lineCtx.strokeStyle = "#e0e0e0";
        lineCtx.lineWidth = 1;
        lineCtx.fillStyle = "#888";
        // Font dinamis: kecil di HP, normal di Desktop
        lineCtx.font = isMobile ? "9px sans-serif" : "11px sans-serif";
        lineCtx.textAlign = "right";

        const steps = 5;
        for (let i = 0; i <= steps; i++) {
          // Hitung posisi Y
          let y =
            height -
            paddingBottom -
            i * ((height - paddingBottom - paddingTop) / steps);

          // Gambar Garis Horizontal
          lineCtx.moveTo(paddingLeft, y);
          lineCtx.lineTo(width, y);

          // Teks Nominal
          let valLabel = ((maxVal * 1.1) / steps) * i;
          // Di HP teks "Rp" bisa dihilangkan jika terlalu sempit, tapi "M" penting
          let text = isMobile
            ? valLabel.toFixed(1) + "M"
            : formatRupiah(valLabel);
          lineCtx.fillText(text, paddingLeft - 8, y + 3);
        }
        lineCtx.stroke();

        // 2. Grafik Garis
        lineCtx.beginPath();
        const gradientStroke = lineCtx.createLinearGradient(0, 0, width, 0);
        gradientStroke.addColorStop(0, "#2ecc71");
        gradientStroke.addColorStop(1, "#f1c40f");

        lineCtx.strokeStyle = gradientStroke;
        lineCtx.lineWidth = isMobile ? 3 : 4; // Garis lebih tipis di HP
        lineCtx.lineJoin = "round";

        const currentIdxFloat = (progress / 100) * totalPoints;
        const currentIdxInt = Math.floor(currentIdxFloat);

        let currentDataVal = dataValues[Math.min(currentIdxInt, totalPoints)];
        displayOmset.innerText = "Rp " + currentDataVal.toFixed(1) + " M";

        lineCtx.moveTo(getX(0), getY(dataValues[0]));
        for (let i = 0; i < totalPoints; i++) {
          if (i < currentIdxInt) {
            lineCtx.lineTo(getX(i + 1), getY(dataValues[i + 1]));
          } else if (i === currentIdxInt) {
            const decimal = currentIdxFloat - i;
            const cx = getX(i) + (getX(i + 1) - getX(i)) * decimal;
            const cy =
              getY(dataValues[i]) +
              (getY(dataValues[i + 1]) - getY(dataValues[i])) * decimal;
            lineCtx.lineTo(cx, cy);
          }
        }
        lineCtx.stroke();

        // 3. Area Fill
        if (progress > 0) {
          lineCtx.lineTo(
            getX(Math.min(currentIdxInt + 1, totalPoints)),
            height - paddingBottom
          );
          lineCtx.lineTo(getX(0), height - paddingBottom);
          const fillGrad = lineCtx.createLinearGradient(0, 0, 0, height);
          fillGrad.addColorStop(0, "rgba(39, 174, 96, 0.2)");
          fillGrad.addColorStop(1, "rgba(255,255,255,0)");
          lineCtx.fillStyle = fillGrad;
          lineCtx.fill();
        }

        // 4. X-Axis Labels (Bulan) - RESPONSIVE LOGIC
        lineCtx.fillStyle = "#666";
        lineCtx.textAlign = "center";
        lineCtx.font = isMobile ? "9px sans-serif" : "11px sans-serif";

        // LOGIKA PENTING: Jika HP, loncat label lebih banyak (step 4 atau 6). Desktop step 1 atau 2.
        let labelStep;
        if (width < 480) labelStep = 4; // HP Kecil: Tampilkan setiap 4 bulan
        else if (width < 768) labelStep = 3; // Tablet/HP Besar: Setiap 3 bulan
        else labelStep = 1; // Desktop: Semua bulan

        for (let i = 0; i <= currentIdxInt; i++) {
          // Tampilkan label jika kelipatan step ATAU jika itu titik terakhir
          if (i % labelStep === 0 || i === totalPoints) {
            // Highlight label tahun baru
            if (labels[i].includes("Jan"))
              lineCtx.font =
                "bold " + (isMobile ? "10px" : "12px") + " sans-serif";
            else lineCtx.font = isMobile ? "9px sans-serif" : "11px sans-serif";

            lineCtx.fillText(labels[i], getX(i), height - paddingBottom + 20);
          }

          // Gambar Dot Putih di ujung
          if (i === currentIdxInt) {
            let lx = getX(i) + (getX(i + 1) - getX(i)) * (currentIdxFloat - i);
            let ly =
              getY(dataValues[i]) +
              (getY(dataValues[i + 1]) - getY(dataValues[i])) *
                (currentIdxFloat - i);
            if (currentIdxInt === totalPoints) {
              lx = getX(totalPoints);
              ly = getY(dataValues[totalPoints]);
            }

            lineCtx.beginPath();
            lineCtx.fillStyle = "#fff";
            lineCtx.strokeStyle = "#27ae60";
            lineCtx.lineWidth = 2;
            lineCtx.arc(lx, ly, isMobile ? 4 : 6, 0, Math.PI * 2);
            lineCtx.fill();
            lineCtx.stroke();
          }
        }

        // --- B. PIE CHART ---
        pieCtx.clearRect(0, 0, pieCanvas.width, pieCanvas.height);
        const cx = pieCanvas.width / 2,
          cy = pieCanvas.height / 2;
        const r = pieCanvas.width / 2 - 5;
        let startAng = -0.5 * Math.PI;
        const maxAng = (progress / 100) * (2 * Math.PI);
        let curAng = 0;

        pieData.forEach((slice) => {
          const sliceAng = (slice.v / 100) * (2 * Math.PI);
          if (curAng < maxAng) {
            let visSlice =
              curAng + sliceAng > maxAng ? maxAng - curAng : sliceAng;
            pieCtx.beginPath();
            pieCtx.moveTo(cx, cy);
            pieCtx.arc(cx, cy, r, startAng, startAng + visSlice);
            pieCtx.fillStyle = slice.c;
            pieCtx.fill();
            // Border putih antar slice
            pieCtx.strokeStyle = "#fff";
            pieCtx.lineWidth = 2;
            pieCtx.stroke();

            startAng += visSlice;
            curAng += visSlice;
          }
        });
        // Donut Hole
        pieCtx.beginPath();
        pieCtx.arc(cx, cy, r * 0.55, 0, 2 * Math.PI);
        pieCtx.fillStyle = "white";
        pieCtx.fill();

        // --- LOOP LOGIC ---
        if (progress < 100) {
          progress += 0.5;
          requestAnimationFrame(animate);
        } else {
          isPaused = true;
          setTimeout(() => {
            progress = 0;
            isPaused = false;
            requestAnimationFrame(animate);
          }, 3000);
        }
      }

      window.onload = animate;
    </script>
  </body>
</html>
