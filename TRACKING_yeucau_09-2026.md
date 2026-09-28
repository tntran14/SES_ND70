# Tracking yêu cầu chỉnh sửa (theo 3 file docx) — 2026-09-15

Trạng thái chung (cập nhật 2026-09-28): **ĐÃ CODE XONG mục 1, 2, 3, 4** và đã có trên GitHub, nhánh `dev` (commit `979a86e`, `210c961`, `bf7841e`, `94797a8`, `4879d4a` + commit rà soát 2026-09-28). Còn lại: 2 file biên bản `-MULTI`/`-MULTI-101` (nếu có dùng), copy các file `.jrxml` đã sửa lên server, và các điểm cần anh xác nhận ở mục "Rà soát 2026-09-28". Chưa build/test thực tế trên môi trường đủ dependency.

> Lưu ý (ghi chú cũ, đã lỗi thời): lúc khảo sát có 9 file sửa dở (tính năng "Hàng hóa đặc trưng" LHHDTrung/SKhung/SMay/BKSPTVChuyen + refactor HttpClient→HttpURLConnection trong `CommonController.java`). **Thực tế các file này đã bị commit chung vào `979a86e`**, kèm theo `hddt SES-api/pom.xml` và `hddt SES-api/src/main/resources/mongodb.properties` (cấu hình máy cá nhân) — xem mục "Rà soát 2026-09-28".

---

## 1. Chỉnh logic biên bản và điều chỉnh/thay thế hoá đơn

Nguồn: `LBBDCTTheImpl.java`, `LBBDCTTheCRUDController.java`, `JMSListenerImpl.java`, `EInvoiceImpl.java`, view `bbdctthe/*.html`

- [x] **Q1 đã trả lời**: chỉ bỏ điều kiện bắt buộc **hóa đơn mới** (thay thế/điều chỉnh) phải tồn tại + đã ký. Hóa đơn gốc (bị thay thế/điều chỉnh, "hóa đơn bị sai") vẫn phải tồn tại/đã ký như cũ vì đó là hóa đơn lỗi đã phát hành. Không cần thêm hóa đơn mới ngay khi tạo biên bản.
- [x] 1.1 `LBBDCTTheImpl.java` case CREATE: đã bỏ yêu cầu bắt buộc `HDNew` phải có ngay khi tạo — chỉ giữ bắt buộc `HDon` (hóa đơn gốc, đã ký/COMPLETE/ADJUSTED/REPLACED). Khi không có `HDNew`: bỏ qua lookup/validate hóa đơn mới, không ghi field `HDDCTThe` vào bản ghi (để trống hẳn, không tạo object rỗng — tránh UI hiện dòng rác) và không ghi vào file XML.
- [x] 1.2 `LBBDCTTheImpl.java` case UPDATE (MODIFY): cho phép bổ sung `HDNew` sau — có gửi `HDNew` thì validate và ghi đè `HDDCTThe`. **Sửa 2026-09-28**: form luôn gửi toàn bộ lưới hóa đơn mới, nên không có `HDNew` nghĩa là user đã xóa dòng → nay `$unset` `HDDCTThe` để DB khớp với file XML biên bản (trước đó XML bị ghi lại không còn hóa đơn mới nhưng DB vẫn giữ).
- [x] 1.3 UI `bbdctthe-crud.html`: **không cần sửa** — rà lại thấy view đã hỗ trợ sẵn (2 grid độc lập, nút "add-inv1" để bổ sung hóa đơn mới sau, không có validate JS chặn grid rỗng). Chốt chặn thực ra nằm ở tầng controller (`LBBDCTTheCRUDController.checkDataToSave`, xem 1.1) — đã sửa ở đó.
- [x] 1.4 ~~Đã bỏ điều kiện ký trên hóa đơn mới~~ **Đính chính 2026-09-28**: code không bỏ điều kiện này. Hóa đơn mới là *không bắt buộc* khi tạo/sửa biên bản, nhưng **khi có thì vẫn phải là hóa đơn đã phát hành** (`EInvoiceStatus` COMPLETE/ADJUSTED/REPLACED, có MCCQT); nút "add-inv1" cũng chỉ cho chọn từ danh sách hóa đơn đã ký (`/common/show-search-list-invoices-signed`). Luồng đúng: tạo biên bản với hóa đơn gốc trước → ký hóa đơn mới → sửa biên bản bổ sung hóa đơn mới. Điều kiện trên hóa đơn gốc giữ nguyên.
- [x] 1.5 Đã sửa `BIEN-BAN-DIEU-CHINH-THAY-THE.jrxml` (anh gửi đúng file, dòng 95-98): thay 4 dòng căn cứ pháp lý cũ (Luật QLT 2019, NĐ 123/2020, NĐ 70/2025, TT 32/2025) bằng 1 dòng duy nhất: *"Căn cứ Thông tư 91/2026/TT-BTC quy định một số điều của Luật Quản lý thuế, hướng dẫn thi hành Nghị định số 254/2026/NĐ-CP của Chính phủ quy định chi tiết một số điều về hóa đơn điện tử, chứng từ điện tử, có hiệu lực kể từ ngày 01/07/2026."* (đúng theo docx yêu cầu, "bỏ cái cũ đi"). Rà toàn file không còn chỗ nào khác trích dẫn văn bản pháp lý cũ.
  - ⚠️ **Lưu ý**: file có 2 biến thể dùng cho in nhiều hóa đơn cùng lúc — `BIEN-BAN-DIEU-CHINH-THAY-THE-MULTI.jrxml` và `BIEN-BAN-DIEU-CHINH-THAY-THE-MULTI-101.jrxml` (dùng trong `LBBDCTTheClientSideImpl.java`) — **anh chưa gửi 2 file này**, nếu chúng cũng có đoạn căn cứ pháp lý tương tự thì cần sửa luôn, báo tôi khi có file.
  - File đã sửa nằm ở gốc repo (`BIEN-BAN-DIEU-CHINH-THAY-THE.jrxml`, commit `210c961`) — repo **không load template từ đây**, code load file từ thư mục `SystemParams.DIR_E_INVOICE_TEMPLATE` trên server lúc chạy, nên anh cần tự copy file đã sửa lên đúng thư mục đó trên server.
- [x] 1.6 Đã kiểm tra luồng auto-create biên bản (`JMSListenerImpl.createBBDCTT`) — chỉ chạy khi hóa đơn mới đã ký xong (đúng nghiệp vụ hiện tại), giữ nguyên không chỉnh.

> **Cập nhật phạm vi (sau khi anh xác nhận Q7)**: đã áp dụng cho cả 2 màn hình — 1-1 chuẩn (`bbdctthe-crud.html`, case `CREATED`/`MODIFY`) và nhiều hóa đơn (`bbdctthe-crud-invs.html`, case `CREATE_BBDCTT`/`MODIFY_BBDCTT`).

## 2. Chỉnh số tiền thuế và thành tiền làm tròn, không thập phân

Nguồn: `einvoice-crud.js`, `einvoice1-crud.js`, `EInvoiceImpl.java`, `Commons.java`, `TKDSHDonImpl.java`, `TKDSHDonBHImpl.java`

- [x] 2.1 `einvoice-crud.js`: đã làm tròn `Total`/`VATAmount`/`Amount` từng dòng và tổng đầu bằng `.toFixed(soThapPhan)` (soThapPhan = 0 nếu VND, giữ 4 nếu ngoại tệ để không sai lệch số tiền quy đổi).
- [x] 2.2 `einvoice1-crud.js` (form không VAT): đã sửa tương tự.
- [x] 2.3 `EInvoiceImpl.java` method `crud()`: đã làm tròn phía server trước khi ghi DB và XML, áp dụng cho cả 3 nhánh mẫu hóa đơn — chỉ làm tròn khi `LoaiTienTt = VND` (thêm hàm `Commons.ToNumberRoundedByCurrency`/`ToNumberStringRoundedByCurrency` để giữ đúng số thập phân cho hóa đơn ngoại tệ).
- [x] 2.4 `Commons.java`: đã thêm `ToNumberRounded`, `ToNumberRoundedByCurrency`, `ToNumberStringRoundedByCurrency`.
- [x] 2.5 `TKDSHDonImpl.java` — báo cáo **tổng quát** (`exportExceGeneral`): đã thêm style số `#,##0;(#,##0)` (`cellStyleNumFooterAmount`) + `Math.round` cho 3 dòng tổng (Tổng doanh thu, Tổng thuế GTGT x2). Đây là nguyên nhân chính khớp với ảnh lỗi trong file docx.
- [x] 2.6 `TKDSHDonBHImpl.java` (dòng ~1401): đã sửa tương tự cho dòng "Tổng doanh thu". **Không** bật lại đoạn tổng thuế GTGT đang bị comment (1412-1437) — đó là thay đổi nội dung báo cáo, ngoài phạm vi "làm tròn"; để riêng nếu anh cần.
- [x] 2.7 Báo cáo **chi tiết** (`exportExcelDSHDCTiet`): đã có style `#,##0` sẵn, dữ liệu nguồn nay đã làm tròn từ 2.3 → không cần sửa thêm.

> Q5 đã trả lời: KHÔNG rà thêm các báo cáo khác ngoài phạm vi — đã tuân thủ, không đụng tới TKDSHDonPXKDLImpl/PXKNBImpl/MTTImpl/MISA và các luồng import Excel (importExcel/importExcelAuto/importExcelMisa).

## 3. Chỉnh thông báo sai sót 04 SS

Nguồn: `JPUtils.java` (print04/viewpdf), `Constants.java`, `custom.hddt.js`, `TBHDSSotMTTImpl.java`, `TBHDSSotMTTSendMailController.java`

> **Q3 đã trả lời**: text chốt = *"Theo Nghị định số 254/2026/NĐ-CP và Thông tư 91/2026/TT-BTC"*.

- [x] 3.1 `JPUtils.java` dòng 1863 và 1975: đã sửa text → `"Hóa đơn điện tử theo Nghị định số 254/2026/NĐ-CP và Thông tư 91/2026/TT-BTC"`.
- [x] 3.2 `hddt-SES/.../Constants.java` dòng 224 (`MAP_HDSS_LOAI_AD_HDDT`, key "1"): đã sửa.
- [x] 3.3 `custom.hddt.js` dòng 20 (`objLADHDDT`): đã sửa.
- [ ] 3.4 Key "5" — **KHÔNG sửa**, ngoài phạm vi (theo Q5).
- [x] 3.5 `TBHDSSotMTTImpl.sendMail`: đã thêm `$lookup`+`$unwind` trên collection `Issuer` (dùng `header.getIssuerId()`, giống pattern `CommonImpl.print04_mtt`).
- [x] 3.6 `TBHDSSotMTTSendMailController.java`: đã thêm dòng "5. Lý do: {lido}" vào nội dung mail.
- [x] 3.7 Đã kiểm tra bản song song (hóa đơn thường, không MTT) — **có cùng lỗi**, đã sửa cả 2:
  - `TBHDSSotSendMailController.java`: thêm dòng "5. Lý do" giống 3.6.
  - `TBHDSSotImpl.sendMail` (dòng ~4244): thiếu `$lookup` Issuer (đã thêm) **và** có thêm 1 lỗi nặng hơn — `$project` giới hạn chỉ giữ `DSHDon/Dir/IssuerId/NTBao` khiến cắt mất cả `EInvoiceDetail`, `UserConFig`, `TNNT`, `MST`, `Loai`... trước khi các bước sau dùng tới → đã bỏ `$project` này (đồng bộ với bản MTT vốn không có project này).
- [ ] 3.8 Template `04SS.jrxml` ngoài git — giữ nguyên phạm vi ở data field `LADHDDT` (3.1), không đào sâu thêm.

---

## 4. Số hộ chiếu (SHChieu) trên hóa đơn GTGT thông thường + bỏ "TT 78" — 2026-09-28

Quy định: thẻ `SHChieu` trong `NMua`, chuỗi, tối đa 20 ký tự. Phạm vi: **chỉ luồng hóa đơn GTGT thông thường** (`einvoice`, `EInvoiceImpl`).

- [x] 4.1 Form `einvoice/einvoice-crud.html` đã có sẵn ô "Số hộ chiếu" (`kh-shchieu`), lưu XML + DB (`NMua.SHChieu`) ở tạo/sửa/sao chép và import Excel `importExcelAuto` — chỉ bổ sung `maxlength="20"` + kiểm tra ≤ 20 ký tự ở `EInvoiceCRUDController` (commit `bf7841e`).
- [x] 4.2 Dữ liệu cho Jasper: `JPUtils.createFinalInvoice` (SES-api và SES-pdf) truyền tham số tên **`NMuaSHChieu`**. Mẫu `INV-MULTI-VAT-2026-SOHOCHIEU.jrxml` đọc nhầm `$P{SHChieu}` nên không in → đổi thành `$P{NMuaSHChieu}` (`bf7841e`); `INV-MULTI-VAT-2026.jrxml` thêm dòng "Số hộ chiếu (Passport No.)", chỉ in khi có dữ liệu (`4879d4a`). Hai file giờ giống nhau. **Cần copy lên thư mục template trên server.**
- [x] 4.3 Thứ tự thẻ `NMua` ở sửa/sao chép/`importExcelAuto` đưa về giống lúc tạo: `Ten, MST, DChi, MDVQHNSach, MKHang, SDThoai, CCCDan, SHChieu, DCTDTu, HVTNMHang, STKNHang, TNHang`; sửa tên thẻ sai `MSVQHNSNM` → `MDVQHNSach` trong `importExcelAuto` (`94797a8`).
- [x] 4.4 `importExcelAuto`: trim số hộ chiếu, > 20 ký tự thì dừng import và báo danh sách Mã HĐ lỗi (`94797a8`).
- [x] 4.5 `THDon` hóa đơn GTGT thông thường: "Hóa đơn giá trị gia tăng TT 78" → "Hóa đơn giá trị gia tăng" (form tạo mới + 6 chỗ import Excel trong `EInvoiceImpl`) (`4879d4a`).
- [ ] 4.6 Ngoài phạm vi, chưa làm (chờ anh quyết): "TT 78" còn ở `EInvoiceMTTImpl` (2 chỗ), `EInvoiceImpl1` (import hóa đơn bán hàng lại ghi "Hóa đơn giá trị gia tăng TT 78"), form bán hàng "Hóa đơn bán hàng 78", tên file Excel mẫu `*-TT78.xlsx`. `importExcel`/`importExcelMisa` không ghi `MDVQHNSach/CCCDan/SHChieu` vì mẫu Excel không có cột.

---

## Rà soát 2026-09-28 (đối chiếu source trên `dev` với file này)

Đã sửa trong đợt rà soát:
- **R1** `Commons.ToNumberStringRoundedByCurrency` (mục 2.3): với hóa đơn ngoại tệ, hàm đổi chuỗi qua `double` rồi `String.valueOf` → số ≥ 10.000.000 bị ghi dạng mũ (`1.250000025E7`) vào XML `ThTien`/`VATAmount`/`Amount` (sai kiểu decimal, dễ gặp với KRW/JPY). Ngoài ra dòng không có thành tiền (ghi chú) bị đổi từ rỗng thành `0`. Đã sửa: ngoại tệ trả lại đúng chuỗi gốc (bỏ dấu phẩy), chuỗi rỗng giữ rỗng, VND vẫn làm tròn.
- **R2** `LBBDCTTheImpl` MODIFY 1-1 (mục 1.2): `$unset HDDCTThe` khi không còn hóa đơn mới (xem 1.2).

Đã kiểm tra, đúng:
- Mục 1: tạo/sửa biên bản chỉ với hóa đơn gốc ở cả 2 màn hình; in PDF biên bản (`printbb`) xử lý được khi thiếu `HDDCTThe`; căn cứ pháp lý `BIEN-BAN-DIEU-CHINH-THAY-THE.jrxml` đúng câu trong docx.
- Mục 2: làm tròn form JS (VND 0 số lẻ), server khi ghi XML/DB, footer báo cáo tổng quát (`#,##0`). Lưu ý nhỏ: ngoại tệ tiền thuế trên form trước đây hiển thị 1 số lẻ, nay 4 số lẻ (ghi "giữ nguyên quy tắc cũ" ở 2.1 là chưa chính xác).
- Mục 3: text 04/SS đổi đủ ở `JPUtils` (2 chỗ), `Constants` (hddt-SES), `custom.hddt.js`; `$lookup` Issuer + dòng "Lý do" trong mail ở cả bản thường và MTT.
- `CommonController`: refactor `HttpClient` → `HttpURLConnection` là cần thiết (module `hddt-SES` build Java 1.8, không có `java.net.http`).

**Anh đã trả lời (2026-09-28):**
- **C1 — ĐÃ SỬA**: trả `mongodb.properties` của SES-api về DB server (`103.144.86.137` / `hddt-ses`) như trước `979a86e`; `pom.xml` SES-api đổi `systemPath` jar font sang `${basedir}/lib-ext/jasperreports-fonts-ext.jar` (giống SES-pdf, không phụ thuộc máy). Nội dung cũ: cấu hình bị commit nhầm trong `979a86e`: `hddt SES-api/src/main/resources/mongodb.properties` đổi từ DB server (`103.144.86.137` / `hddt-ses`) sang DB máy cá nhân (`127.0.0.1` / `hddt-demo`); `hddt SES-api/pom.xml` đổi `systemPath` jar font theo đường dẫn máy cá nhân. Module `SES-pdf` vẫn trỏ DB server. Nếu build/deploy API từ `dev` sẽ kết nối sai DB. Có trả 2 file này về như trước `979a86e` không?
- **C2 — anh chọn chuyển repo sang private** (anh tự làm trên GitHub; vẫn nên đổi mật khẩu DB/ActiveMQ vì đã lộ trong lịch sử git). Nội dung cũ: repo đang public, `mongodb.properties` (2 module) và `activemq.properties` chứa mật khẩu dạng rõ, nằm cả trong lịch sử git. Nên đổi mật khẩu DB/ActiveMQ và chuyển repo sang private.
- **C3 — GIỮ NGUYÊN hiện tại.** Nội dung cũ: Hóa đơn mới trên biên bản có cần cho phép chọn hóa đơn *chưa ký* không (xem đính chính 1.4)? Hiện chỉ chọn được hóa đơn đã ký.
- **C4 — GIỮ NGUYÊN.** Nội dung cũ (MTT, commit `126a9a3`): import Excel MTT với Tính chất = 5 nhưng loại hàng hóa đặc trưng khác 2 sẽ sinh thẻ `<TTHHDTrung/>` rỗng — có thể bị CQT từ chối.

---

## Câu hỏi đã trả lời (Q1, Q2, Q3, Q4, Q5)

- ✅ Q1, Q3, Q4, Q5: đã trả lời — nội dung được gộp trực tiếp vào từng mục việc ở trên.
- ✅ **Q2 — đã xong (commit `210c961`)**, nội dung cũ: file template anh vừa thêm (4 file `.jrxml` ở gốc repo) không phải file biên bản. Cần đúng file `BIEN-BAN-DIEU-CHINH-THAY-THE.jrxml` (và nếu có dùng bản in nhiều hóa đơn thì thêm `BIEN-BAN-DIEU-CHINH-THAY-THE-MULTI.jrxml`, `BIEN-BAN-DIEU-CHINH-THAY-THE-MULTI-101.jrxml`) lấy từ thư mục template thật trên server. Việc 1.5 sẽ tạm để sau, các việc khác (1.1-1.4, 1.6, mục 2, mục 3) không phụ thuộc vào file này nên có thể làm trước.

---

## Cần anh xác nhận thêm (phát sinh trong lúc code)

- ✅ **Q6 đã xác nhận: đúng** — chỉ làm tròn khi VND, giữ nguyên ngoại tệ. Không cần sửa thêm.
- ✅ **Q7 đã xác nhận: có làm** — đã áp dụng logic mới (cho tạo trước, không bắt buộc đủ cặp cũ/mới) cho cả màn hình nhiều hóa đơn `bbdctthe-crud-invs.html` (case `CREATE_BBDCTT`/`MODIFY_BBDCTT` trong `LBBDCTTheImpl.java`, ghép theo từng dòng — hóa đơn mới ở vị trí nào chưa có thì bỏ qua dòng đó khi ghi `HDDCTThe`). **Đính chính 2026-09-28**: controller vẫn chặn trường hợp nhập thiếu — danh sách hóa đơn mới phải *rỗng* hoặc *đủ bằng* số hóa đơn sai sót; không cho nhập một phần. Đã gộp lại validate ở `LBBDCTTheCRUDController.checkDataToSave` cho cả 4 transaction (`lbbdctt-cre/-invs`, `lbbdctt-edit/-invs`) dùng chung 1 rule.

---

## Log tiến độ
- 2026-09-15: Khảo sát xong source (3 agent song song), viết file tracking này, gửi list cho user duyệt. Chưa code.
- 2026-09-15: User trả lời Q1/Q3/Q4/Q5, thêm 4 file jrxml cho Q2 nhưng không đúng file cần (là mẫu hóa đơn/PXK/TNCN, không phải mẫu biên bản). Đã xác định đúng tên file cần: `BIEN-BAN-DIEU-CHINH-THAY-THE.jrxml`. Chờ user gửi đúng file hoặc xác nhận bỏ qua việc 1.5. Chưa code.
- 2026-09-15: User duyệt "code đi". Đã code xong:
  - **Mục 3** (3.1-3.3, 3.5-3.7): sửa text căn cứ pháp lý 04/SS ở `JPUtils.java`, `Constants.java`, `custom.hddt.js`; thêm `$lookup` Issuer bị thiếu trong `TBHDSSotMTTImpl.sendMail` và `TBHDSSotImpl.sendMail` (bản này còn có thêm lỗi `$project` cắt mất field, đã bỏ luôn); bổ sung dòng "Lý do" bị thiếu vào nội dung mail ở cả 2 controller send-mail (MTT và thường).
  - **Mục 2** (2.1-2.7): làm tròn `Total`/`VATAmount`/`Amount` ở form JS (`einvoice-crud.js`, `einvoice1-crud.js`) và server (`EInvoiceImpl.java`, thêm helper trong `Commons.java`) — **chỉ làm tròn khi hóa đơn VND**, giữ nguyên số thập phân cho hóa đơn ngoại tệ để không làm sai lệch số tiền quy đổi (phát hiện thêm trong lúc code, không có trong bản duyệt ban đầu — cần anh xác nhận lại hướng này). Sửa footer báo cáo Excel tổng quát (`TKDSHDonImpl.java`, `TKDSHDonBHImpl.java`) thêm style số + làm tròn.
  - **Mục 1** (1.1-1.4, 1.6): sửa `LBBDCTTheImpl.java` (case CREATE/MODIFY) và `LBBDCTTheCRUDController.java` (check-data-save) để cho tạo/sửa biên bản chỉ với hóa đơn gốc, hóa đơn mới bổ sung sau không bắt buộc + không bắt buộc đã ký. Đã xử lý kỹ để tránh NPE và tránh mất dữ liệu khi update. (Ghi chú lúc đó: chỉ màn hình 1-1; sau Q7 đã áp dụng cho cả màn hình nhiều hóa đơn `-invs`.)
  - Không build thử được (thiếu 1 file jar cục bộ cấu hình theo path máy khác trong pom.xml, môi trường issue có sẵn từ trước, không phải do sửa đổi) — đã soát bằng tay (đọc lại toàn bộ đoạn sửa, so sánh cân bằng ngoặc trước/sau qua git diff) nhưng **chưa chạy thử thực tế trên máy có đủ môi trường**.
  - Còn lại lúc đó: 1.5 (sau đã xong ở `210c961`) và Q6/Q7 (đã xác nhận).
- 2026-09-22: Commit `210c961` (mục 1.5 căn cứ pháp lý biên bản) và `126a9a3` (Tính chất hàng hóa đặc trưng + Biển số xe cho import Excel MTT) — đã có trên GitHub.
- 2026-09-28: Mục 4 (số hộ chiếu SHChieu, thứ tự thẻ NMua, bỏ "TT 78" hóa đơn GTGT) — commit `bf7841e`, `94797a8`, `4879d4a`, merge vào `dev`.
- 2026-09-28: Rà soát toàn bộ file này với source: sửa R1, R2; đính chính 1.2, 1.4, Q7, trạng thái chung; ghi các điểm C1-C4 cần anh xác nhận. Đã kiểm tra cú pháp Java (javac) các file sửa; chưa build đầy đủ (không tải được thư viện Maven).
- 2026-09-28: Anh trả lời C1-C4. Sửa C1 (DB SES-api trỏ về server, pom dùng `${basedir}`); C2 anh tự chuyển repo sang private; C3, C4 giữ nguyên.
