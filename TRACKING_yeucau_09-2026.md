# Tracking yêu cầu chỉnh sửa (theo 3 file docx) — 2026-09-15

Trạng thái chung: **ĐÃ CODE XONG toàn bộ mục 1, 2, 3** (kể cả 1.5). Đã commit + push lên GitHub (`https://github.com/tntran14/SES_ND70.git`, branch `dev`, commit `979a86e` + 1 commit tiếp theo cho 1.5). Còn lại: file template MULTI/-101 (nếu cần), và anh tự copy file `BIEN-BAN-DIEU-CHINH-THAY-THE.jrxml` đã sửa lên server. Chưa build/test thực tế trên môi trường đủ dependency.

> Lưu ý quan trọng: nhánh `dev` hiện có 9 file đang sửa dở (uncommitted), nhưng khảo sát xác nhận **không liên quan** đến 3 yêu cầu dưới đây — đó là tính năng "Hàng hóa đặc trưng" (LHHDTrung/SKhung/SMay/BKSPTVChuyen...) đang làm dở + 1 refactor HttpClient→HttpURLConnection trong CommonController.java. Sẽ không đụng vào các file đó trừ khi anh yêu cầu.

---

## 1. Chỉnh logic biên bản và điều chỉnh/thay thế hoá đơn

Nguồn: `LBBDCTTheImpl.java`, `LBBDCTTheCRUDController.java`, `JMSListenerImpl.java`, `EInvoiceImpl.java`, view `bbdctthe/*.html`

- [x] **Q1 đã trả lời**: chỉ bỏ điều kiện bắt buộc **hóa đơn mới** (thay thế/điều chỉnh) phải tồn tại + đã ký. Hóa đơn gốc (bị thay thế/điều chỉnh, "hóa đơn bị sai") vẫn phải tồn tại/đã ký như cũ vì đó là hóa đơn lỗi đã phát hành. Không cần thêm hóa đơn mới ngay khi tạo biên bản.
- [x] 1.1 `LBBDCTTheImpl.java` case CREATE: đã bỏ yêu cầu bắt buộc `HDNew` phải có ngay khi tạo — chỉ giữ bắt buộc `HDon` (hóa đơn gốc, đã ký/COMPLETE/ADJUSTED/REPLACED). Khi không có `HDNew`: bỏ qua lookup/validate hóa đơn mới, không ghi field `HDDCTThe` vào bản ghi (để trống hẳn, không tạo object rỗng — tránh UI hiện dòng rác) và không ghi vào file XML.
- [x] 1.2 `LBBDCTTheImpl.java` case UPDATE (MODIFY): cho phép bổ sung `HDNew` sau — chỉ validate/ghi đè field `HDDCTThe` khi client có gửi `HDNew` lên; nếu không gửi, giữ nguyên dữ liệu cũ (không bị `$set` đè mất) để tránh mất dữ liệu khi user chỉ sửa các trường khác (lý do, nội dung...).
- [x] 1.3 UI `bbdctthe-crud.html`: **không cần sửa** — rà lại thấy view đã hỗ trợ sẵn (2 grid độc lập, nút "add-inv1" để bổ sung hóa đơn mới sau, không có validate JS chặn grid rỗng). Chốt chặn thực ra nằm ở tầng controller (`LBBDCTTheCRUDController.checkDataToSave`, xem 1.1) — đã sửa ở đó.
- [x] 1.4 Đã bỏ điều kiện `SignStatusCode/EInvoiceStatus` bắt buộc trên **hóa đơn mới** tại thời điểm tạo/sửa biên bản. Điều kiện ký trên hóa đơn gốc giữ nguyên (nằm trong 1.1/1.2).
- [x] 1.5 Đã sửa `BIEN-BAN-DIEU-CHINH-THAY-THE.jrxml` (anh gửi đúng file, dòng 95-98): thay 4 dòng căn cứ pháp lý cũ (Luật QLT 2019, NĐ 123/2020, NĐ 70/2025, TT 32/2025) bằng 1 dòng duy nhất: *"Căn cứ Thông tư 91/2026/TT-BTC quy định một số điều của Luật Quản lý thuế, hướng dẫn thi hành Nghị định số 254/2026/NĐ-CP của Chính phủ quy định chi tiết một số điều về hóa đơn điện tử, chứng từ điện tử, có hiệu lực kể từ ngày 01/07/2026."* (đúng theo docx yêu cầu, "bỏ cái cũ đi"). Rà toàn file không còn chỗ nào khác trích dẫn văn bản pháp lý cũ.
  - ⚠️ **Lưu ý**: file có 2 biến thể dùng cho in nhiều hóa đơn cùng lúc — `BIEN-BAN-DIEU-CHINH-THAY-THE-MULTI.jrxml` và `BIEN-BAN-DIEU-CHINH-THAY-THE-MULTI-101.jrxml` (dùng trong `LBBDCTTheClientSideImpl.java`) — **anh chưa gửi 2 file này**, nếu chúng cũng có đoạn căn cứ pháp lý tương tự thì cần sửa luôn, báo tôi khi có file.
  - File đã sửa vẫn nằm ở gốc repo (`D:\Program Files\ses-invoice-2025\BIEN-BAN-DIEU-CHINH-THAY-THE.jrxml`) — repo này **không lưu template**, code load file từ thư mục `SystemParams.DIR_E_INVOICE_TEMPLATE` trên server lúc chạy, nên anh cần tự copy file đã sửa lên đúng thư mục đó trên server.
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

## Câu hỏi đã trả lời (Q1, Q3, Q4, Q5) / còn chờ (Q2)

- ✅ Q1, Q3, Q4, Q5: đã trả lời — nội dung được gộp trực tiếp vào từng mục việc ở trên.
- ⚠️ **Q2 — còn vướng**: file template anh vừa thêm (4 file `.jrxml` ở gốc repo) không phải file biên bản. Cần đúng file `BIEN-BAN-DIEU-CHINH-THAY-THE.jrxml` (và nếu có dùng bản in nhiều hóa đơn thì thêm `BIEN-BAN-DIEU-CHINH-THAY-THE-MULTI.jrxml`, `BIEN-BAN-DIEU-CHINH-THAY-THE-MULTI-101.jrxml`) lấy từ thư mục template thật trên server. Việc 1.5 sẽ tạm để sau, các việc khác (1.1-1.4, 1.6, mục 2, mục 3) không phụ thuộc vào file này nên có thể làm trước.

---

## Cần anh xác nhận thêm (phát sinh trong lúc code)

- ✅ **Q6 đã xác nhận: đúng** — chỉ làm tròn khi VND, giữ nguyên ngoại tệ. Không cần sửa thêm.
- ✅ **Q7 đã xác nhận: có làm** — đã áp dụng logic mới (cho tạo trước, không bắt buộc đủ cặp cũ/mới) cho cả màn hình nhiều hóa đơn `bbdctthe-crud-invs.html` (case `CREATE_BBDCTT`/`MODIFY_BBDCTT` trong `LBBDCTTheImpl.java`, ghép theo từng dòng — hóa đơn mới ở vị trí nào chưa có thì bỏ qua dòng đó khi ghi `HDDCTThe`, không bắt buộc cả danh sách). Đã gộp lại validate ở `LBBDCTTheCRUDController.checkDataToSave` cho cả 4 transaction (`lbbdctt-cre/-invs`, `lbbdctt-edit/-invs`) dùng chung 1 rule.

---

## Log tiến độ
- 2026-09-15: Khảo sát xong source (3 agent song song), viết file tracking này, gửi list cho user duyệt. Chưa code.
- 2026-09-15: User trả lời Q1/Q3/Q4/Q5, thêm 4 file jrxml cho Q2 nhưng không đúng file cần (là mẫu hóa đơn/PXK/TNCN, không phải mẫu biên bản). Đã xác định đúng tên file cần: `BIEN-BAN-DIEU-CHINH-THAY-THE.jrxml`. Chờ user gửi đúng file hoặc xác nhận bỏ qua việc 1.5. Chưa code.
- 2026-09-15: User duyệt "code đi". Đã code xong:
  - **Mục 3** (3.1-3.3, 3.5-3.7): sửa text căn cứ pháp lý 04/SS ở `JPUtils.java`, `Constants.java`, `custom.hddt.js`; thêm `$lookup` Issuer bị thiếu trong `TBHDSSotMTTImpl.sendMail` và `TBHDSSotImpl.sendMail` (bản này còn có thêm lỗi `$project` cắt mất field, đã bỏ luôn); bổ sung dòng "Lý do" bị thiếu vào nội dung mail ở cả 2 controller send-mail (MTT và thường).
  - **Mục 2** (2.1-2.7): làm tròn `Total`/`VATAmount`/`Amount` ở form JS (`einvoice-crud.js`, `einvoice1-crud.js`) và server (`EInvoiceImpl.java`, thêm helper trong `Commons.java`) — **chỉ làm tròn khi hóa đơn VND**, giữ nguyên số thập phân cho hóa đơn ngoại tệ để không làm sai lệch số tiền quy đổi (phát hiện thêm trong lúc code, không có trong bản duyệt ban đầu — cần anh xác nhận lại hướng này). Sửa footer báo cáo Excel tổng quát (`TKDSHDonImpl.java`, `TKDSHDonBHImpl.java`) thêm style số + làm tròn.
  - **Mục 1** (1.1-1.4, 1.6): sửa `LBBDCTTheImpl.java` (case CREATE/MODIFY) và `LBBDCTTheCRUDController.java` (check-data-save) để cho tạo/sửa biên bản chỉ với hóa đơn gốc, hóa đơn mới bổ sung sau không bắt buộc + không bắt buộc đã ký. Đã xử lý kỹ để tránh NPE và tránh mất dữ liệu khi update. **Chỉ áp dụng cho màn hình 1-1 chuẩn** (`bbdctthe-crud.html`), **không đụng** màn hình nhiều hóa đơn `-invs`.
  - Không build thử được (thiếu 1 file jar cục bộ cấu hình theo path máy khác trong pom.xml, môi trường issue có sẵn từ trước, không phải do sửa đổi) — đã soát bằng tay (đọc lại toàn bộ đoạn sửa, so sánh cân bằng ngoặc trước/sau qua git diff) nhưng **chưa chạy thử thực tế trên máy có đủ môi trường**.
  - Còn lại: 1.5 (blocked, chờ đúng file jrxml), và 2 điểm cần anh xác nhận thêm (xem "Cần xác nhận thêm" bên dưới).
