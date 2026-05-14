# Keep navigation Safe Args generated classes
-keep class com.pal.ussd.ui.progress.ProgressFragmentArgs { *; }
-keep class com.pal.ussd.ui.transfer.TransferFragmentDirections { *; }

# Keep encrypted shared preferences
-keep class androidx.security.crypto.** { *; }
