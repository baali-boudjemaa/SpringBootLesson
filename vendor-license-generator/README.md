# Rawdati vendor license generator

This is a vendor-only tool. Keep it and the private key outside the customer
application and never send the private key to a customer.

## Generate a customer activation key

1. Copy the machine ID from the customer's activation screen.
2. Keep `private_key.der` in a secure, non-versioned location, for example
   `C:\Secure\Rawdati\private_key.der`.
3. From the repository root, run:

```powershell
.\gradlew.bat run --args='"CUSTOMER_MACHINE_ID" "C:\Secure\Rawdati\private_key.der" MONTHLY'
```

4. Select `MONTHLY` for one calendar month or `YEARLY` for one calendar year.
   Send the printed activation key to the customer. It works only for that
   machine ID and expires automatically.

## Key safety

The customer app must ship only `src/main/resources/license/public_key.der`.
Do not ship or commit a `private_key.der`. The current repository contains
legacy private-key copies; move the private key to a secure location and rotate
the key pair before distributing the application.
