# Backend Wallet Core

Backend core xử lý ví điện tử bằng Java thuần, cung cấp các nghiệp vụ tạo ví, nạp tiền, rút tiền, chuyển tiền, kiểm tra số dư và truy vấn lịch sử giao dịch. Dữ liệu được lưu trong bộ nhớ và triển khai theo mô hình phân tách domain, repository và service.

## Công nghệ

- Java 21
- Maven
- JUnit 5.14.4
- Maven Surefire 3.2.5

## Tính năng

- Tạo ví với mã khách hàng và đơn vị tiền tệ
- Nạp tiền và rút tiền
- Chuyển tiền giữa hai ví
- Kiểm tra số dư và lịch sử giao dịch
- Hỗ trợ trạng thái ví: `ACTIVE`, `FROZEN`, `CLOSED`
- Hỗ trợ trạng thái giao dịch: `SUCCESS`, `FAILED`
- Trừ/chặn số dư trước khi hoàn tất chuyển tiền
- Hoàn tác ví nguồn nếu ví đích không thể nhận tiền
- Lưu giao dịch thất bại kèm lý do
- Từ chối số tiền âm hoặc bằng 0
- Từ chối giao dịch khi ví không tồn tại, bị khóa hoặc ví nguồn/ví đích khác đơn vị tiền tệ

## Cấu trúc thư mục

```text
src/
├── main/java/com/company/wallet/
│   ├── domain/          # Wallet, Money và các thực thể giao dịch
│   ├── exception/       # Các ngoại lệ nghiệp vụ của wallet
│   ├── repository/      # Interface và implementation lưu dữ liệu trong bộ nhớ
│   ├── service/         # Xử lý logic nghiệp vụ WalletService
│   └── Main.java        # Entry point của ứng dụng
└── test/java/com/company/wallet/service/
    └── WalletServiceTest.java
```

## Yêu cầu

- JDK 21 trở lên
- Apache Maven 3.x

Kiểm tra các công cụ đã cài đặt:

```bash
java -version
mvn -version
```

## Cài đặt và chạy

Clone repository:

```bash
git clone https://github.com/dangtrungghieu/Backend-Wallet-Core.git
cd Backend-Wallet-Core
```

Chạy ứng dụng:

```bash
mvn compile
mvn exec:java -Dexec.mainClass="com.company.wallet.Main"
```

Lưu ý: `Main.java` hiện tại chưa chứa code khởi chạy cụ thể.

## Kiểm thử

Chạy toàn bộ test bằng Maven:

```bash
mvn test
```

Bộ test hiện tại tập trung kiểm tra `WalletService`, bao gồm:

- Tạo ví và khởi tạo số dư
- Nạp/rút tiền hợp lệ và không hợp lệ
- Chuyển tiền hợp lệ
- Chuyển cho chính ví nguồn
- Chuyển tiền vượt số dư
- Ví bị khóa
- Ví không tồn tại
- Giao dịch khác đơn vị tiền tệ
- Ghi nhận giao dịch thành công/thất bại

## Ví dụ sử dụng

```java
WalletRepository walletRepository = new InMemoryWalletRepository();
TransactionRepository transactionRepository = new InMemoryTransactionRepository();
WalletService walletService = new WalletService(walletRepository, transactionRepository);

Wallet source = walletService.createWallet("CUSTOMER001", "VND");
Wallet destination = walletService.createWallet("CUSTOMER002", "VND");

walletService.deposit(source.getWalletId(), new Money("100000", "VND"));
walletService.transfer(
    source.getWalletId(),
    destination.getWalletId(),
    new Money("20000", "VND")
);

Money balance = walletService.getBalance(source.getWalletId());
List<Transaction> history = walletService.getTransactionHistory(source.getWalletId());
```

> Constructor `Money` trong dự án yêu cầu `BigDecimal` hoặc `double`, thay vì `String`. Ví dụ minh họa có thể thay bằng `new Money(new BigDecimal("100000"), "VND")`.

## Kiến trúc

- `domain`: chứa dữ liệu và quy tắc nghiệp vụ cốt lõi.
- `repository`: cung cấp abstraction cho thao tác lưu trữ ví và giao dịch.
- `service`: điều phối nghiệp vụ, kiểm tra điều kiện và ghi lịch sử giao dịch.
- `exception`: mô tả các lỗi nghiệp vụ có ý nghĩa.

Repository hiện được triển khai bằng bộ nhớ; dự án chưa cấu hình cơ sở dữ liệu hoặc REST API.

## Đóng góp

1. Fork repository
2. Tạo nhánh mới: `git checkout -b feature/ten-tinh-nang`
3. Commit thay đổi: `git commit -m "feat: mô tả thay đổi"`
4. Push nhánh: `git push origin feature/ten-tinh-nang`
5. Tạo Pull Request

## Giấy phép

Repository hiện chưa khai báo license.
