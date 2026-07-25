use jni::objects::{JClass, JString};
use jni::sys::jint;
use jni::JNIEnv;

use std::sync::OnceLock;
use byteorder::{BigEndian, LittleEndian, ByteOrder};
use memmap2::Mmap;
use log::{error, info};

const RECORD_SIZE: usize = 13;
static MMAP: OnceLock<Mmap> = OnceLock::new();

fn init_mmap(db_path: &str) -> Result<(), Box<dyn std::error::Error>> {
    let file = std::fs::File::open(db_path)?;
    let mmap = unsafe { Mmap::map(&file)? };
    MMAP.set(mmap).map_err(|_| "Already initialized")?;
    Ok(())
}

fn binary_search(barcode: u64, buf: &[u8]) -> Option<usize> {
    let count = buf.len() / RECORD_SIZE;
    let mut lo = 0;
    let mut hi = count;
    while lo < hi {
        let mid = lo + (hi - lo) / 2;
        let offset = mid * RECORD_SIZE;
        let key = BigEndian::read_u64(&buf[offset..offset+8]);
        if key < barcode {
            lo = mid + 1;
        } else {
            hi = mid;
        }
    }
    if lo < count {
        let offset = lo * RECORD_SIZE;
        let key = BigEndian::read_u64(&buf[offset..offset+8]);
        if key == barcode {
            return Some(offset);
        }
    }
    None
}

fn lookup(barcode: u64) -> jint {
    let mmap = MMAP.get().expect("mmap not initialized");
    match binary_search(barcode, mmap.as_ref()) {
        Some(offset) => {
            let rating = mmap[offset + 8] as u32;
            let flags = LittleEndian::read_u32(&mmap[offset + 9..offset + 13]);
            // Упаковываем и преобразуем в i32 (jint)
            let packed = ((rating & 0xFF) << 24) | (flags & 0xFF_FFFF);
            packed as i32
        }
        None => -1,
    }
}

#[no_mangle]
pub extern "system" fn JNI_OnLoad(
    _vm: jni::JavaVM,
    _reserved: *mut std::os::raw::c_void,
) -> jni::sys::jint {
    android_logger::init_once(
        android_logger::Config::default()
            .with_max_level(log::LevelFilter::Trace)
            .with_tag("product_lib"),
    );
    info!("Rust library loaded, android_logger initialized");
    jni::sys::JNI_VERSION_1_6
}

#[no_mangle]
pub extern "system" fn Java_com_scanner_app_data_NativeLib_lookupProduct(
    mut env: JNIEnv,
    _class: JClass,
    barcode: JString,
    db_path: JString,
) -> jint {
    if barcode.is_null() || db_path.is_null() {
        error!("lookupProduct called with null argument");
        let _ = env.throw_new("java/lang/IllegalArgumentException", "null argument");
        return -1;
    }

    let barcode_str = match env.get_string(&barcode) {
        Ok(s) => s,
        Err(e) => {
            error!("Failed to get barcode string: {:?}", e);
            return -1;
        }
    };
    let barcode_rust = match barcode_str.to_str() {
        Ok(s) => s.trim(),
        Err(e) => {
            error!("Barcode is not valid UTF-8: {:?}", e);
            return -1;
        }
    };
    let barcode_num: u64 = match barcode_rust.parse() {
        Ok(n) => n,
        Err(e) => {
            error!("Cannot parse barcode '{}': {:?}", barcode_rust, e);
            return -1;
        }
    };

    let db_path_str = match env.get_string(&db_path) {
        Ok(s) => s,
        Err(e) => {
            error!("Failed to get db_path string: {:?}", e);
            return -1;
        }
    };
    let db_path_rust = match db_path_str.to_str() {
        Ok(s) => s,
        Err(_) => {
            error!("db_path is not valid UTF-8");
            let _ = env.throw_new("java/lang/IllegalArgumentException", "path not UTF-8");
            return -1;
        }
    };

    if MMAP.get().is_none() {
        if let Err(e) = init_mmap(db_path_rust) {
            error!("Failed to mmap file at {}: {:?}", db_path_rust, e);
            return -1;
        }
    }

    let packed = lookup(barcode_num);
    if packed != 0 {
        info!("Found barcode {} -> rating/flags {:08X}", barcode_num, packed);
    }
    packed
}
