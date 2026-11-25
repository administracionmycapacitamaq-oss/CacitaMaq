package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.R
import com.example.data.model.AdminConfig
import com.example.data.model.Certificate
import com.google.firebase.FirebaseApp
import org.json.JSONObject
import java.io.ByteArrayOutputStream

object WebVerifierHtmlBuilder {

    private fun escapeHtml(raw: String): String {
        return raw
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun encodeDrawableToDataUri(context: Context, resId: Int, maxSizePx: Int = 260): String {
        return try {
            val raw = BitmapFactory.decodeResource(context.resources, resId) ?: return ""
            val scale = maxSizePx.toFloat() / maxOf(raw.width, raw.height).toFloat()
            val resized = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    raw,
                    (raw.width * scale).toInt().coerceAtLeast(1),
                    (raw.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                raw
            }
            val baos = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 88, baos)
            val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$b64"
        } catch (_: Exception) {
            ""
        }
    }

    private fun resolveFirebaseEndpointConfig(context: Context): Pair<String, String> {
        val dbId = runCatching { context.getString(R.string.firestore_database_id) }.getOrDefault("")
        val options = runCatching { FirebaseApp.getInstance().options }.getOrNull()
        val projectId = options?.projectId.orEmpty()
        return projectId to dbId
    }

    fun buildSingleCertificateHtml(context: Context, certificate: Certificate): String {
        val sealDataUri = encodeDrawableToDataUri(context, R.drawable.img_capacitamaq_seal_1795460461997, 260)
        val bannerDataUri = encodeDrawableToDataUri(context, R.drawable.img_capacitamaq_banner_1795460462074, 240)
        val initialPayload = JSONObject().apply {
            put("c", certificate.certificateCode)
            put("t", certificate.courseName)
            put("d", certificate.description)
            put("s", certificate.studentName)
            put("r", certificate.studentRut)
            put("ct", certificate.certificateType)
            put("e", certificate.equipment)
            put("i", certificate.issuer)
            put("ia", certificate.issuedAt)
            put("ea", certificate.expiresAt)
            put("st", certificate.computedStatus().label)
            put("stKey", certificate.computedStatus().name)
            put("on", certificate.operatorName)
            put("or", certificate.operatorRole)
            put("w", certificate.whatsapp)
            put("em", certificate.email)
            put("vb", certificate.verifiedByAdminEmail.ifBlank { AdminConfig.OFFICIAL_ADMIN_EMAIL })
            put("tc", certificate.themeColor)
        }.toString()

        val (projectId, dbId) = resolveFirebaseEndpointConfig(context)
        return buildTemplateHtml(
            sealDataUri = sealDataUri,
            bannerDataUri = bannerDataUri,
            embeddedInitialJson = initialPayload,
            pageTitle = "Verificación · ${escapeHtml(certificate.studentName.ifBlank { certificate.certificateCode })}",
            firebaseProjectId = projectId,
            firestoreDbId = dbId
        )
    }

    fun buildUniversalVerifierHtml(context: Context): String {
        val sealDataUri = encodeDrawableToDataUri(context, R.drawable.img_capacitamaq_seal_1795460461997, 260)
        val bannerDataUri = encodeDrawableToDataUri(context, R.drawable.img_capacitamaq_banner_1795460462074, 240)
        val (projectId, dbId) = resolveFirebaseEndpointConfig(context)
        return buildTemplateHtml(
            sealDataUri = sealDataUri,
            bannerDataUri = bannerDataUri,
            embeddedInitialJson = "null",
            pageTitle = "Verificador Oficial · CapacitaMaq",
            firebaseProjectId = projectId,
            firestoreDbId = dbId
        )
    }

    private fun buildTemplateHtml(
        sealDataUri: String,
        bannerDataUri: String,
        embeddedInitialJson: String,
        pageTitle: String,
        firebaseProjectId: String,
        firestoreDbId: String
    ): String {
        val cfgJson = JSONObject().apply {
            put("projectId", firebaseProjectId)
            put("databaseId", firestoreDbId)
            put("adminEmail", AdminConfig.OFFICIAL_ADMIN_EMAIL)
        }.toString()

        return """<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0" />
<title>$pageTitle</title>
<style>
  :root {
    --navy: #0e2c57;
    --blue: #1554a0;
    --gold: #f3c970;
    --cream: #fffcf4;
    --border: #d9c48f;
    --text: #101e32;
    --muted: #64748b;
  }
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    background: linear-gradient(160deg, #071933 0%, #0e2c57 52%, #1554a0 100%);
    color: var(--text);
    min-height: 100vh;
    padding: 20px 14px 36px;
    display: flex;
    justify-content: center;
    align-items: flex-start;
  }
  .shell {
    width: 100%;
    max-width: 480px;
  }
  .card {
    background: var(--cream);
    border: 2px solid var(--border);
    border-radius: 22px;
    overflow: hidden;
    box-shadow: 0 20px 48px rgba(4, 14, 31, 0.42);
  }
  .brand-bar {
    background: #ffffff;
    padding: 10px 16px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    border-bottom: 1px solid #e2e8f0;
  }
  .brand-bar img {
    height: 34px;
    width: auto;
    object-fit: contain;
  }
  .brand-tag {
    font-size: 10px;
    font-weight: 800;
    letter-spacing: 1px;
    color: var(--blue);
    text-transform: uppercase;
    text-align: right;
  }
  .hero-band {
    height: 82px;
    background: var(--theme, var(--gold));
    position: relative;
  }
  .seal-wrap {
    display: flex;
    justify-content: center;
    margin-top: -48px;
    position: relative;
    z-index: 2;
  }
  .seal {
    width: 96px;
    height: 96px;
    border-radius: 50%;
    background: #ffffff;
    border: 4px solid #ffffff;
    box-shadow: 0 6px 18px rgba(14, 44, 87, 0.25);
    object-fit: contain;
    padding: 3px;
  }
  .body {
    padding: 14px 20px 22px;
    text-align: center;
  }
  .kicker {
    font-size: 11px;
    font-weight: 800;
    letter-spacing: 1.6px;
    color: #a18140;
    text-transform: uppercase;
  }
  .title {
    margin-top: 4px;
    font-size: 22px;
    font-weight: 800;
    color: var(--navy);
  }
  .badge {
    display: inline-block;
    margin-top: 10px;
    padding: 6px 14px;
    border-radius: 999px;
    font-size: 12px;
    font-weight: 800;
    letter-spacing: 0.6px;
  }
  .badge-valid { background: #dcfce7; color: #166534; border: 1px solid #86efac; }
  .badge-pending { background: #fef3c7; color: #92400e; border: 1px solid #fcd34d; }
  .badge-invalid { background: #fee2e2; color: #991b1b; border: 1px solid #fca5a5; }
  .live-status {
    margin-top: 10px;
    padding: 7px 11px;
    border-radius: 10px;
    font-size: 11px;
    font-weight: 700;
    line-height: 1.35;
  }
  .live-checking { background: #eff6ff; color: #1e40af; border: 1px solid #bfdbfe; }
  .live-ok { background: #ecfdf5; color: #065f46; border: 1px solid #a7f3d0; }
  .live-revoked {
    background: #fef2f2;
    color: #991b1b;
    border: 1.5px solid #f87171;
    text-align: left;
    display: flex;
    gap: 10px;
    align-items: flex-start;
  }
  .live-offline {
    background: #fffbeb;
    color: #92400e;
    border: 1.5px solid #f59e0b;
    text-align: left;
    display: flex;
    gap: 10px;
    align-items: flex-start;
  }
  .live-banner-icon { flex-shrink: 0; margin-top: 1px; }
  .live-banner-title {
    font-size: 11.5px;
    font-weight: 800;
    text-transform: uppercase;
    letter-spacing: 0.4px;
    margin-bottom: 2px;
  }
  .live-banner-desc { font-size: 11px; font-weight: 600; line-height: 1.4; }
  .btn-retry {
    margin-top: 8px;
    display: inline-block;
    background: #d97706;
    color: #ffffff;
    border: 0;
    border-radius: 8px;
    padding: 7px 12px;
    font-size: 11px;
    font-weight: 800;
    cursor: pointer;
  }
  .divider {
    width: 90px;
    height: 2px;
    background: var(--border);
    margin: 14px auto;
  }
  .student-label { font-size: 12px; color: var(--muted); }
  .student-name {
    margin-top: 4px;
    font-size: 21px;
    font-weight: 800;
    color: var(--navy);
    text-transform: uppercase;
  }
  .student-rut {
    margin-top: 6px;
    display: inline-block;
    background: #eef4fc;
    color: var(--navy);
    border: 1px solid #cbd9ec;
    padding: 4px 12px;
    border-radius: 8px;
    font-size: 13px;
    font-weight: 700;
  }
  .course-name {
    margin-top: 12px;
    font-size: 16px;
    font-weight: 800;
    color: #9b7427;
    text-transform: uppercase;
    line-height: 1.35;
  }
  .desc {
    margin-top: 6px;
    font-size: 12px;
    color: var(--muted);
    line-height: 1.45;
  }
  .meta-box {
    margin-top: 16px;
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 14px;
    padding: 12px 14px;
    text-align: left;
  }
  .meta-row {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    padding: 6px 0;
    border-bottom: 1px solid #f1f5f9;
    font-size: 12.5px;
  }
  .meta-row:last-child { border-bottom: none; }
  .meta-k { color: var(--muted); font-weight: 600; flex-shrink: 0; }
  .meta-v { color: var(--text); font-weight: 700; text-align: right; word-break: break-word; }
  .admin-proof {
    margin-top: 14px;
    background: #eff6ff;
    border: 1px solid #bfdbfe;
    border-radius: 12px;
    padding: 10px 12px;
    display: flex;
    align-items: center;
    gap: 10px;
    text-align: left;
  }
  .admin-proof img {
    width: 42px;
    height: 42px;
    border-radius: 8px;
    background: #fff;
    object-fit: contain;
    flex-shrink: 0;
  }
  .admin-proof-title { font-size: 10.5px; font-weight: 800; color: var(--blue); text-transform: uppercase; }
  .admin-proof-mail {
    font-size: 11.5px;
    font-weight: 700;
    color: var(--navy);
    word-break: normal;
    overflow-wrap: anywhere;
  }
  .actions {
    margin-top: 14px;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
  .btn {
    flex: 1 1 120px;
    display: inline-block;
    padding: 11px 12px;
    border-radius: 12px;
    font-size: 13px;
    font-weight: 700;
    text-decoration: none;
    text-align: center;
    border: 0;
    cursor: pointer;
  }
  .btn-wa { background: #16a34a; color: #ffffff; }
  .btn-mail { background: var(--navy); color: #ffffff; }
  .btn-print { background: #e2e8f0; color: var(--navy); }
  .btn-primary { background: var(--blue); color: #ffffff; width: 100%; margin-top: 8px; }
  .lookup-box {
    margin-top: 14px;
    text-align: left;
  }
  .lookup-input {
    width: 100%;
    padding: 11px 12px;
    border-radius: 10px;
    border: 1px solid #cbd5e1;
    font-size: 14px;
    background: #ffffff;
  }
  .empty {
    padding: 18px 8px 6px;
    text-align: center;
    color: var(--muted);
    font-size: 13.5px;
    line-height: 1.5;
  }
  @media print {
    body { background: #ffffff; padding: 0; }
    .card { box-shadow: none; }
    .actions { display: none; }
  }
</style>
</head>
<body>
  <div class="shell">
    <div class="card" id="cardRoot">
      <div class="brand-bar">
        <img src="$bannerDataUri" alt="CapacitaMaq" />
        <div class="brand-tag">Verificación Web Oficial</div>
      </div>
      <div class="hero-band" id="heroBand"></div>
      <div class="seal-wrap">
        <img class="seal" src="$sealDataUri" alt="Sello oficial CapacitaMaq" />
      </div>
      <div class="body" id="cardBody"></div>
    </div>
  </div>
<script>
(function() {
  var embedded = $embeddedInitialJson;
  var fbCfg = $cfgJson;

  function esc(v) {
    return String(v || '').replace(/[&<>"']/g, function(ch) {
      return ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'})[ch];
    });
  }

  function decodeBase64Url(str) {
    var b64 = str.replace(/-/g, '+').replace(/_/g, '/');
    while (b64.length % 4) b64 += '=';
    var bin = atob(b64);
    var bytes = new Uint8Array(bin.length);
    for (var i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
    return new TextDecoder('utf-8').decode(bytes);
  }

  function formatSpanishDate(iso) {
    var raw = String(iso || '').trim();
    if (!raw) return 'Sin vencimiento';
    if (!/^\d{4}-\d{2}-\d{2}/.test(raw)) return raw;
    var parts = raw.slice(0, 10).split('-');
    var months = ['enero','febrero','marzo','abril','mayo','junio','julio','agosto','septiembre','octubre','noviembre','diciembre'];
    var mIdx = parseInt(parts[1], 10) - 1;
    var day = parseInt(parts[2], 10);
    if (mIdx < 0 || mIdx > 11 || isNaN(day)) return raw;
    return day + ' de ' + months[mIdx] + ' de ' + parts[0];
  }

  function computeStatus(rawStatus, expiresAt) {
    if (rawStatus === 'invalid' || rawStatus === 'revoked' || rawStatus === 'INVALID') {
      return { key: 'INVALID', label: 'NO VÁLIDA' };
    }
    if (rawStatus === 'pending' || rawStatus === 'PENDING') {
      return { key: 'PENDING', label: 'PENDIENTE' };
    }
    var exp = String(expiresAt || '').trim();
    if (/^\d{4}-\d{2}-\d{2}/.test(exp)) {
      var today = new Date().toISOString().slice(0, 10);
      if (exp.slice(0, 10) < today) {
        return { key: 'EXPIRED', label: 'VENCIDA' };
      }
    }
    return { key: 'VALID', label: 'CERTIFICADA' };
  }

  function extractCodeFromUrl() {
    var full = window.location.href;
    var markers = ['#verificar/', '/verificar/', '?c=', '&c='];
    for (var i = 0; i < markers.length; i++) {
      var m = markers[i];
      var pos = full.indexOf(m);
      if (pos !== -1) {
        var sub = full.slice(pos + m.length).split('?')[0].split('&')[0].split('#')[0];
        try { sub = decodeURIComponent(sub); } catch (_) {}
        if (sub && sub.trim()) return sub.trim();
      }
    }
    return '';
  }

  function extractPayloadFromUrl() {
    var full = window.location.href;
    var idx = full.indexOf('?v=');
    if (idx === -1) idx = full.indexOf('#v=');
    if (idx === -1) idx = full.indexOf('&v=');
    if (idx === -1) return null;
    var raw = full.slice(idx + 3).split('&')[0].split('#')[0];
    try { raw = decodeURIComponent(raw); } catch (_) {}
    try {
      var p = JSON.parse(decodeBase64Url(raw));
      var computed = computeStatus(p.st || 'VALID', p.ea || '');
      return {
        c: p.c || '',
        t: p.t || '',
        d: p.d || '',
        s: p.s || '',
        r: p.r || '',
        ct: p.ct || 'Certificación de competencia',
        e: p.e || '',
        i: p.i || 'CAPACITAMAQ',
        ia: p.ia || '',
        ea: p.ea || '',
        st: computed.label,
        stKey: computed.key,
        on: p.on || '',
        or: p.or || '',
        w: p.w || '',
        em: p.em || '',
        vb: p.vb || fbCfg.adminEmail,
        tc: p.tc || '#f3c970'
      };
    } catch (_) {
      return null;
    }
  }

  function firestoreString(fields, key) {
    return (fields && fields[key] && fields[key].stringValue) ? fields[key].stringValue : '';
  }

  function buildLiveBannerHtml(liveBannerClass, liveBannerText) {
    if (!liveBannerText && liveBannerClass !== 'live-offline') return '';
    if (liveBannerClass === 'live-offline') {
      return '<div class="live-status live-offline">' +
        '<svg class="live-banner-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#b45309" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
          '<line x1="2" y1="2" x2="22" y2="22"></line>' +
          '<path d="M8.5 16.5a5 5 0 0 1 7 0"></path>' +
          '<path d="M2 8.82a15 15 0 0 1 4.17-2.65"></path>' +
          '<path d="M10.66 5c4.01-.36 8.14.9 11.34 3.76"></path>' +
          '<path d="M16.85 11.25a10 10 0 0 1 2.22 1.68"></path>' +
          '<path d="M5 12.86a10 10 0 0 1 5.17-2.89"></path>' +
          '<line x1="12" y1="20" x2="12.01" y2="20"></line>' +
        '</svg>' +
        '<div>' +
          '<div class="live-banner-title">Sin conexión a internet (Modo Offline)</div>' +
          '<div class="live-banner-desc">' + esc(liveBannerText || 'Sin acceso a Firebase en vivo. Los datos provienen del respaldo cifrado.') + '</div>' +
          '<button type="button" id="retryLiveBtn" class="btn-retry">Reintentar verificación en vivo</button>' +
        '</div>' +
      '</div>';
    }
    if (liveBannerClass === 'live-revoked') {
      return '<div class="live-status live-revoked">' +
        '<svg class="live-banner-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#991b1b" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
          '<circle cx="12" cy="12" r="10"></circle>' +
          '<line x1="15" y1="9" x2="9" y2="15"></line>' +
          '<line x1="9" y1="9" x2="15" y2="15"></line>' +
        '</svg>' +
        '<div>' +
          '<div class="live-banner-title">Certificado no encontrado o retirado</div>' +
          '<div class="live-banner-desc">' + esc(liveBannerText) + '</div>' +
        '</div>' +
      '</div>';
    }
    return '<div class="live-status ' + liveBannerClass + '">' + esc(liveBannerText) + '</div>';
  }

  function renderCertificate(data, liveBannerClass, liveBannerText) {
    var body = document.getElementById('cardBody');
    var hero = document.getElementById('heroBand');
    if (data.tc) hero.style.background = data.tc;

    var badgeClass = data.stKey === 'VALID' ? 'badge-valid' : (data.stKey === 'PENDING' ? 'badge-pending' : 'badge-invalid');
    var waDigits = String(data.w || '').replace(/\D/g, '');
    var issuedDisplay = data.ia ? formatSpanishDate(data.ia) : '-';
    var expiresDisplay = data.ea ? formatSpanishDate(data.ea) : 'Sin vencimiento';

    var html = '' +
      '<div class="kicker">CERTIFICACIÓN VERIFICABLE</div>' +
      '<div class="title">Certificado de reconocimiento</div>' +
      '<span class="badge ' + badgeClass + '">' + esc(data.st) + '</span>' +
      buildLiveBannerHtml(liveBannerClass, liveBannerText) +
      '<div class="divider"></div>' +
      '<div class="student-label">Se certifica que</div>' +
      '<div class="student-name">' + esc(data.s || 'Alumno certificado') + '</div>' +
      (data.r ? '<div class="student-rut">RUT: ' + esc(data.r) + '</div>' : '') +
      '<div class="course-name">' + esc(data.t) + '</div>' +
      (data.d ? '<div class="desc">' + esc(data.d) + '</div>' : '') +
      '<div class="meta-box">' +
        '<div class="meta-row"><span class="meta-k">Código</span><span class="meta-v">' + esc(data.c) + '</span></div>' +
        '<div class="meta-row"><span class="meta-k">Entidad</span><span class="meta-v">' + esc(data.i || 'CAPACITAMAQ') + '</span></div>' +
        '<div class="meta-row"><span class="meta-k">Maquinaria</span><span class="meta-v">' + esc(data.e || 'No especificada') + '</span></div>' +
        '<div class="meta-row"><span class="meta-k">Emisión</span><span class="meta-v">' + esc(issuedDisplay) + '</span></div>' +
        '<div class="meta-row"><span class="meta-k">Vigencia</span><span class="meta-v">' + esc(expiresDisplay) + '</span></div>' +
        (data.on ? '<div class="meta-row"><span class="meta-k">Operador cert.</span><span class="meta-v">' + esc(data.on) + (data.or ? ' · ' + esc(data.or) : '') + '</span></div>' : '') +
      '</div>' +
      '<div class="admin-proof">' +
        '<img src="$bannerDataUri" alt="CapacitaMaq" />' +
        '<div>' +
          '<div class="admin-proof-title">Comprobado por administrador oficial</div>' +
          '<div class="admin-proof-mail">' + esc(data.vb || fbCfg.adminEmail) + '</div>' +
        '</div>' +
      '</div>';

    html += '<div class="actions">';
    if (waDigits) {
      html += '<a class="btn btn-wa" href="https://wa.me/' + waDigits + '" target="_blank">WhatsApp</a>';
    }
    if (data.em) {
      html += '<a class="btn btn-mail" href="mailto:' + esc(data.em) + '">Correo</a>';
    }
    html += '<button class="btn btn-print" onclick="window.print()">Imprimir</button>';
    html += '</div>';

    body.innerHTML = html;

    var retryBtn = document.getElementById('retryLiveBtn');
    if (retryBtn) {
      retryBtn.addEventListener('click', function() {
        runVerification();
      });
    }
  }

  function renderLookupHome(alertClass, alertMsg) {
    var body = document.getElementById('cardBody');
    var alertHtml = '';
    if (alertMsg) {
      alertHtml = buildLiveBannerHtml(alertClass || 'live-revoked', alertMsg);
    }
    body.innerHTML = '<div class="kicker">SISTEMA DE VERIFICACIÓN</div>' +
      '<div class="title">Verificador Web CapacitaMaq</div>' +
      '<div class="empty">Escanea un código QR emitido por <strong>CapacitaMaq</strong> o ingresa el código público del certificado para verificar en tiempo real su publicación oficial.</div>' +
      alertHtml +
      '<div class="lookup-box">' +
        '<input id="manualCodeInput" class="lookup-input" type="text" placeholder="Ej. CERTIFICACION-CM-40DE4797D4" />' +
        '<button id="manualLookupBtn" class="btn btn-primary">Verificar en vivo</button>' +
      '</div>';

    var btn = document.getElementById('manualLookupBtn');
    var inp = document.getElementById('manualCodeInput');
    if (btn && inp) {
      btn.addEventListener('click', function() {
        var code = inp.value.trim();
        if (code) {
          window.location.hash = '#verificar/' + encodeURIComponent(code);
          runVerification();
        }
      });
    }
    var retryBtn = document.getElementById('retryLiveBtn');
    if (retryBtn) {
      retryBtn.addEventListener('click', function() {
        runVerification();
      });
    }
  }

  function verifyLiveInFirestore(code, fallbackData) {
    if (!code || !fbCfg.projectId || !fbCfg.databaseId) {
      if (fallbackData) {
        renderCertificate(fallbackData, 'live-offline', 'Certificado verificado por respaldo local CapacitaMaq.');
      }
      return;
    }

    if (typeof navigator !== 'undefined' && navigator.onLine === false) {
      if (fallbackData) {
        renderCertificate(fallbackData, 'live-offline', 'Tu dispositivo está sin conexión a internet. Mostrando datos guardados.');
      } else {
        renderLookupHome('live-offline', 'Tu dispositivo no tiene conexión a internet para consultar el estado en vivo en Firebase.');
      }
      return;
    }

    var endpoint = 'https://firestore.googleapis.com/v1/projects/' +
      encodeURIComponent(fbCfg.projectId) +
      '/databases/' + encodeURIComponent(fbCfg.databaseId) +
      '/documents/public_certificates/' + encodeURIComponent(code);

    fetch(endpoint, { method: 'GET', cache: 'no-store' })
      .then(function(res) {
        if (res.status === 404 || res.status === 403) {
          if (fallbackData) {
            var revoked = Object.assign({}, fallbackData, { st: 'NO PUBLICADA', stKey: 'INVALID' });
            renderCertificate(revoked, 'live-revoked', 'Esta certificación no existe o fue retirada por el administrador oficial (' + fbCfg.adminEmail + ').');
          } else {
            renderLookupHome('live-revoked', 'No se encontró ninguna certificación publicada con el código ' + code + '.');
          }
          return null;
        }
        if (!res.ok) throw new Error('HTTP ' + res.status);
        return res.json();
      })
      .then(function(doc) {
        if (!doc || !doc.fields) return;
        var f = doc.fields;
        var st = firestoreString(f, 'status') || 'active';
        var ea = firestoreString(f, 'expiresAt');
        var computed = computeStatus(st, ea);
        var liveData = {
          c: firestoreString(f, 'certificateCode') || code,
          t: firestoreString(f, 'courseName'),
          d: firestoreString(f, 'description'),
          s: firestoreString(f, 'studentName'),
          r: firestoreString(f, 'studentRut'),
          ct: firestoreString(f, 'certificateType') || 'Certificación de competencia',
          e: firestoreString(f, 'equipment'),
          i: firestoreString(f, 'issuer') || 'CAPACITAMAQ',
          ia: firestoreString(f, 'issuedAt'),
          ea: ea,
          st: computed.label,
          stKey: computed.key,
          on: firestoreString(f, 'operatorName'),
          or: firestoreString(f, 'operatorRole'),
          w: firestoreString(f, 'whatsapp'),
          em: firestoreString(f, 'email'),
          vb: firestoreString(f, 'verifiedByAdminEmail') || fbCfg.adminEmail,
          tc: firestoreString(f, 'themeColor') || '#f3c970'
        };
        renderCertificate(
          liveData,
          computed.key === 'VALID' ? 'live-ok' : 'live-revoked',
          'VERIFICADO EN TIEMPO REAL · Publicado en la nube por ' + liveData.vb
        );
      })
      .catch(function() {
        if (fallbackData) {
          renderCertificate(fallbackData, 'live-offline', 'No fue posible conectarse a Firebase en este momento. Mostrando respaldo firmado.');
        } else {
          renderLookupHome('live-offline', 'Sin conexión a internet para consultar la nube de CapacitaMaq.');
        }
      });
  }

  function runVerification() {
    var urlPayload = extractPayloadFromUrl();
    var code = extractCodeFromUrl() || (embedded && embedded.c) || (urlPayload && urlPayload.c) || '';
    var initial = embedded || urlPayload;

    if (initial) {
      var cObj = computeStatus(initial.stKey || initial.st, initial.ea);
      initial.st = cObj.label;
      initial.stKey = cObj.key;
      renderCertificate(initial, 'live-checking', 'Comprobando publicación en vivo con administrador oficial…');
      if (code) verifyLiveInFirestore(code, initial);
    } else if (code) {
      document.getElementById('cardBody').innerHTML =
        '<div class="title">Consultando CapacitaMaq…</div>' +
        '<div class="live-status live-checking">Verificando publicación oficial en Firebase…</div>';
      verifyLiveInFirestore(code, null);
    } else {
      renderLookupHome('', '');
    }
  }

  window.addEventListener('hashchange', runVerification);
  window.addEventListener('online', runVerification);
  runVerification();
})();
</script>
</body>
</html>"""
    }
}
