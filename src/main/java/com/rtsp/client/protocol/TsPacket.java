package com.rtsp.client.protocol;

/** 용어 정리
 * Multimedia Multiplexing (멀티미디어 다중화)
 *  - 음성, 영상과 같이 특성이 다른 개별 매체상의 정보열을 하나의 채널로 전송할 때, 일정 규칙에 따라 하나의 정보열(단일 비트 스트림 형태)로 만드는 것
 * MPEG-2 다중화
 *  - 복수의 비디오, 오디오, 데이터 스트림(ES)을 하나의 데이터 스트림(PES)으로 다중화
 * ES (Elementary Stream)
 *  - MPEG 에서 영상, 음성 등이 압축된 각 미디어 비트 스트림을 의미함
 * PES (Packetized Elementary Stream, PES Packet)
 *  - PS (Program Stream) 또는 TS (Transport Stream)을 구성하기 위한 직전 단계
 *  - 가변길이로 패킷화한 ES
 * PSI (Program Specific Information, 프로그램 지정 정보)
 *  - MPEG-2 에서 여러 프로그램들로 다중화된 복잡한 TS의 역다중화에 필요한 프로그램 정보를 테이블 형식으로 담아낸 메타데이터
 *  - PAT (Program Association Table, 프로그램 연결)
 *    : TS 에서 사용할 수 있는 모든 프로그램 나열, 채널 내 프로그램 정보 맵 (PMT) 을 유지 한다. (PID=0)
 *  - PMT (Program Map Table, 프로그램 맵)
 *    : 프로그램 내의 영상/음성 스트림에 대한 정보(PID) 를 포함 한다.
 *  - CAT (Conditional Access Table, 조건식 제한 접근)
 *    : 스크램블링과 같은 접근 제어 정보를 포함 한다. (PID=1)
 *  - NIT (Network Information Table, 네트워크 정보)
 *    : MPEG 정보 전송 시 사용된 네트워크 정보를 포함 한다. (PID=10)
 *
 *  -------------------------------------------------------------------------------
 *  TS Packet 생성 과정 (ES -> PES -> TS)                                           |
 *  -------------------------------------------------------------------------------
 *  ES Level    (Elementary Stream)                                               |
 *  -------------------------------------------------------------------------------
 *                  | E1 |      E2    |  E3  |                                    |
 *  -------------------------------------------------------------------------------
 *  PES Level   (Packetized Elementary Stream)                                    |
 *  -------------------------------------------------------------------------------
 *                  |P1| E1 |P2|    E2    |P3|  E3  |                             |
 *  -------------------------------------------------------------------------------
 *  TS Level    (Transport Stream)                                                |
 *  -------------------------------------------------------------------------------
 *                  |4B|    184bytes   |4B|    184bytes   |4B|    184bytes   |    |
 *                  |T1|P1| E1 |   N   |T2|P2|     E2     |T3|P3|  E3  |  N  |    |
 *  -------------------------------------------------------------------------------
 *  Tn : TS Header      /   Pn : PES Header     /   En : ES     /   N : Null data |
 *  -------------------------------------------------------------------------------
 */

import com.rtsp.client.protocol.base.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * @class public class TsPacket
 * @brief TS Packet 클래스
 * @author hyeon seong Lim
 */
public class TsPacket {

    private static final Logger log = LoggerFactory.getLogger(TsPacket.class);

    public static final int TS_TOTAL_SIZE = 188;
    public static final int TS_HEADER_SIZE = 4;
    public static final int TS_BODY_SIZE = 184;
    public static final byte SYNC_BYTE = 0x47;

    /** TS Packet Header Field */
    // TS Packet Header
    private byte[] tsPacketHeader = new byte[0];
    // 동기 비트 0x 47 (0100 0111)
    private byte syncByte = SYNC_BYTE;                          // 8 bits
    // 현재 TS 패킷에 보정 되지 못한 비트 에러의 존재 유 true / 무 false flag
    private boolean transportErrorIndicator = false;            // 1 bit
    // PES(PSI) 시작 여 true / 부 false
    private boolean payloadUnitStartIndicator = false;          // 1 bit
    // 동일한 PID를 갖는 패킷이 존재할 때, 우선 순위 부여 true / 미부여 false
    private boolean transportPriority = false;                  // 1 bit
    // TS 패킷 미디어 식별자
    private String pid = "";                                    // 13 bits
    // 스크램블링 유 01 10 11 / 무 00 및 종류 표시 (01 10 11 의 의미는 규격마다 다름)
    private int transportScramblingControl = 0;                 // 2 bits
    // TS 패킷 해더 다음 바로 Adaptation Field 존재 유무
    // 01 : 유효 필드 X, 페이로드 O         10 : 유효 필드 O, 페이로드 X
    // 11 : 유효 필드 O, 페이로드 O         00 : 유효 필드 X, 페이로드 X
    private int adaptationFieldControl = 0;                     // 2 bits
    // 동일 PID TS 패킷 전송 시 각 TS 패킷 마다 값을 1씩 증가 시킴으로써, 패킷 수신 시 연속된 데이터인지 판단할 수 있는 지표
    private int continuityCounter = 0;                          // 16 bits
    /** TS Packet Header Field END */

    /** Adaptation Field (adaptationFieldControl 이 10 또는 11일 때 존재) */
    // Adaptation Field
    private byte[] adaptationField = new byte[0];
    // Adaptation Field 길이
    private int adaptationFieldLength = 0;                      // 8 bits
//    // continuityCounter가 연속적이지 않으면 1로 설정
//    private boolean discontinuityIndicator = false;             // 1 bit
//    // 오류 없이 디코딩 될 수 있을 때 1
//    private boolean randomAccessIndicator = false;              // 1 bit
//    // ES 우선 순위 부여 1 / 미부여 0
//    private boolean elementaryStreamPriorityIndicator = false;  // 1 bit
//    // PCR 필드 존재 시 설정
//    private boolean pcrFlag = false;                            // 1 bit
//    // OPCR 필드 존재 시 설정
//    private boolean opcrFlag = false;                           // 1 bit
//    // spliceCountdown 필드 존재 시 설정
//    private boolean splicingPointFlag = false;                  // 1 bit
//    // transportPrivateData 필드 존재 시 설정
//    private boolean transportPrivateDataFlag = false;           // 1 bit
//    // adaptationExtension 필드 존재 시 설정
//    private boolean adaptationFieldExtensionFlag = false;       // 1 bit
//
//    /** Adaptation Field : Optional fields */
//    // Program clock 참조 값 : base * 300 + extension
//    private long pcr = 0;                                // 48 bits (33 bits base, 6 bits reserved, 9 bits extension)
//    // Original Program clock 참조, 하나의 TS가 다른 곳으로 복사될 때 도와준다.
//    private long opcr = 0;                               // 48 bits
//    // 이 연결 지점에 발생한 TS 패킷 정도 (2의 보수, 음수일 수 있음)
//    private short spliceCountdown = 0;                	// 8 bits
//    // transportPrivateData 길이
//    private int transportPrivateDataLength = 0;         // 8 bits
//    // PrivateData
//    private byte[] transportPrivateData = null;               // variable
//
//    // Adaptation Field 추가 값
//    private byte[] adaptationExtension = null;                // variable
//        /** Adaptation Field : Optional fields : Adaptation Extension Field */
//        // Adaptation Field 추가 값 길이
//        private int adaptationExtensionLength = 0;                  // 8 bits
//        // LTW Flag Set 존재 여 1 / 0 부
//        private boolean legalTimeWindowFlag = false;                // 1 bit
//        // Piecewise Flag Set 존재 여 1 / 0 부
//        private boolean piecewiseRateFlag = false;                  // 1 bit
//        // Seamless splice Flag Set 존재 여 1 / 0 부
//        private boolean seamlessSpliceFlag = false;                 // 1 bit
//        // 예약된 비트
//        private short reserved = 0;                                 // 5 bits
//        /** Adaptation Field : Optional fields : Adaptation Extension Field : Optional fields */
//        // 패킷이 누락될 수 있는 경우 버퍼 상태를 결정하기 위한 rebroadcasters 에 대한 추가 정보
//        private int ltwFlagSet = 0;                                 // 16 bits (LTW valid flag 1 bit, LTW offset 15 bits)
//        //  LTW 종료 시간을 정의하기 위해 188 바이트 패킷으로 측정된 스트림 속도
//        private int piecewiseFlagSet = 0;                           // 24 bits (Reserved 2 bits, Piecewise rate 22 bits)
//        // Splice type : H.262 스플라이스의 매개변수 / 스플라이스 포인트의 PES DTS
//        private long seamlessSpliceFlagSet = 0;                     // 40 bits (Splice type 4 bits, DTS next access unit 36 bits)
//        /** Adaptation Field : Optional fields : Adaptation Extension Field : Optional fields END */
//        /** Adaptation Field : Optional fields : Adaptation Extension Field END */
//    // 공백 값 항상 0xFF
//    private byte[] stuffingBytes = null;                      // variable
//    /** Adaptation Field : Optional fields END */
    /** Adaptation Field (adaptationFieldControl 이 10 또는 11일 때 존재) END */

    /** TS Packet Payload Field */
    // 페이로드 시작 표시기 (PUSI, payloadUnitStartIndicator) Flag 가 설정된 경우에만 존재
    // 새 페이로드 단위가 시작되는 이 바이트 뒤에 인덱스 제공 (인덱스 이전 모든 페이로드 bytes는 이전 페이로드 유닛의 일부)
    private int payloadPointer = 0;                     // 8 bits
    // 페이로드 내용
    private byte[] payload = new byte[0];               // variable

    /** TS Packet Payload Field END */


    public TsPacket(byte[] data) {
        if (data.length == 188 && data[0] == SYNC_BYTE) {
            tsPacketHeader = new byte[TS_HEADER_SIZE];
            System.arraycopy(data, 0, tsPacketHeader, 0, tsPacketHeader.length);
            int tsHeader = ByteUtil.bytesToInt(tsPacketHeader, true);

            // header parsing
            parsingHeader(tsHeader);
            int index = TS_HEADER_SIZE;

            // adaptationFieldControl 가 10 (2) 또는 11 (3) 인 경우 Adaptation Field 존재
            // adaptationFieldControl 가 01 (1) 또는 11 (3) 인 경우 Payload 존재
            switch (adaptationFieldControl) {
                case 1:
                    parsingPayload(data, index);
                    break;
                case 2:
                    parsingAdaptation(data, index);
                    break;
                case 3:
                    index = parsingAdaptation(data, index);
                    parsingPayload(data, index);
                    break;
                default:
                    break;
            }
        } else {
            syncByte = data[0];
        }
    }

    public byte[] getByteData() {
        byte[] data = new byte[TS_TOTAL_SIZE];

        int index = 0;
        System.arraycopy(tsPacketHeader, 0, data, index, tsPacketHeader.length);
        index += tsPacketHeader.length;

        if (adaptationField.length > 0) {
            byte[] adaptationFieldLengthByteData = ByteUtil.intToBytes(adaptationFieldLength, true);
            System.arraycopy(adaptationFieldLengthByteData, 3, data, index, 1);
            index += 1;

            System.arraycopy(adaptationField, 0, data, index, adaptationField.length);
            index += adaptationField.length;
        }

        if (payload.length > 0) {
            if (payloadUnitStartIndicator) {
                byte[] payloadPointerByteData = ByteUtil.intToBytes(payloadPointer, true);
                System.arraycopy(payloadPointerByteData, 3, data, index, 1);
                index += 1;
            }
            System.arraycopy(payload, 0, data, index, payload.length);
        }

        return data;
    }

    /**
     * @fn private void parsingHeader
     * @brief TS Packet 헤더를 파싱하는 메서드
     * @param tsHeader int 형태의 TS Header
     */
    private void parsingHeader (int tsHeader) {
        this.transportErrorIndicator = ( (tsHeader >>> 0x17) & 0x01 ) == 0x01;
        this.payloadUnitStartIndicator = ( (tsHeader >>> 0x16) & 0x01 ) == 0x01;
        this.transportPriority = ( (tsHeader >>> 0x15) & 0x01 ) == 0x01;
        this.pid = Integer.toBinaryString((tsHeader >>> 0x08) & 0x1FFF);
        this.transportScramblingControl = (tsHeader >>> 0x06) & 0x03;
        this.adaptationFieldControl = (tsHeader >>> 0x04) & 0x03;
        this.continuityCounter = tsHeader & 0x0F;
    }


    // todo
    /**
     * @fn private void parsingAdaptation
     * @brief TS Packet adaptation Field를 파싱하는 메서드
     * @param data TS Packet
     * @param index TS Packet adaptation 시작 지점
     * @return 변경된 index 값
     */
    private int parsingAdaptation (byte[] data, int index) {
        if (data[index] > 0) {
            byte[] adaptationFieldLengthByteData = new byte[1];
            System.arraycopy(data, index, adaptationFieldLengthByteData, 0, adaptationFieldLengthByteData.length);
            this.adaptationFieldLength = adaptationFieldLengthByteData[0];
            index += adaptationFieldLengthByteData.length;

            this.adaptationField = new byte[adaptationFieldLength];
            System.arraycopy(data, index, adaptationField, 0, adaptationField.length);
            index += adaptationField.length;
        }
        return index;
    }

    /**
     * @fn private void parsingPayload
     * @brief TS Packet Payload를 파싱하는 메서드
     * @param data TS Packet
     * @param index TS Packet Payload 시작 지점
     */
    private void parsingPayload (byte[] data, int index) {
        if (payloadUnitStartIndicator) {
            byte[] payloadPointerByteData = new byte[1];
            System.arraycopy(data, index, payloadPointerByteData, 0, payloadPointerByteData.length);
            this.payloadPointer = payloadPointerByteData[0];
            index += payloadPointerByteData.length;
            //log.debug("data[{}] : {}", index, payloadPointer);
        }

        this.payload = new byte[TS_TOTAL_SIZE - index];
        System.arraycopy(data, index, payload, 0, payload.length);
    }

    @Override
    public String toString() {
        return "TsPacket{" +
                "SYNC_BYTE=" + SYNC_BYTE +
                "tsPacketHeader=" + Arrays.toString(tsPacketHeader) +
                ", syncByte=" + syncByte +
                ", transportErrorIndicator=" + transportErrorIndicator +
                ", payloadUnitStartIndicator=" + payloadUnitStartIndicator +
                ", transportPriority=" + transportPriority +
                ", pid='" + pid + "(" + getPidStreamType() + ")" + '\'' +
                ", transportScramblingControl=" + transportScramblingControl +
                ", adaptationFieldControl=" + adaptationFieldControl +
                ", continuityCounter=" + continuityCounter +
                ", adaptationField=" + Arrays.toString(adaptationField) +
                ", adaptationFieldLength=" + adaptationFieldLength +
                ", payloadPointer=" + payloadPointer +
                ", payload=" + Arrays.toString(payload) +
                '}';
    }

    public String print() {
        return "TsPacket{" +
                "SYNC_BYTE=" + SYNC_BYTE +
                "tsPacketHeader=" + Arrays.toString(tsPacketHeader) +
                ", payloadUnitStartIndicator=" + payloadUnitStartIndicator +
                ", adaptationFieldControl=" + adaptationFieldControl +
                ", adaptationField=" + Arrays.toString(adaptationField) +
                ", adaptationFieldLength=" + adaptationFieldLength +
                ", payloadPointer=" + payloadPointer +
                ", payload=" + Arrays.toString(payload) +
                '}';
    }

    public byte[] getPayload() {
        return payload;
    }

    public String getPidStreamType() {
        switch (pid) {
            case "0x00":
                return "ITU-T｜ISO/IEC reserved";
            case "0x01":
                return "ISO/IEC 11172-2 Video";
            case "0x02":
                return "ITU-T Rec. H262｜ISO/IEC 13818-2 Video or ISO/IEC 11172 constrained parameter video stream";
            case "0x03":
                return "ISO/IEC 11172-3 Audio";
            case "0x04":
                return "ISO/IEC 13818-3 Audio";
            case "0x05":
                return "ITU-T Rec. H.222.0｜ISO/IEC 13818-1 private section";
            case "0x06":
                return "ITU-T Rec. H.222.0｜ISO/IEC 13818-1 PES packets containing private data";
            case "0x07":
                return "ISO/IEC 13522 MHEG";
            case "0x08":
                return "Annex A - DSM CC";
            case "0x09":
                return "ITU-T Rec. H.222.1";
            case "0x0A":
                return "ISO/IEC 13818-6 type A";
            case "0x0B":
                return "ISO/IEC 13818-6 type B";
            case "0x0C":
                return "ISO/IEC 13818-6 type C";
            case "0x0D":
                return "ISO/IEC 13818-6 type D";
            case "0x0E":
                return "ISO/IEC 13818-1 auxiliary";
            case "0x0F":
            case "0x10": case "0x11": case "0x12": case "0x13": case "0x14": case "0x15": case "0x16": case "0x17": case "0x18": case "0x19": case "0x1A": case "0x1B": case "0x1C": case "0x1D": case "0x1E": case "0x1F":
            case "0x20": case "0x21": case "0x22": case "0x23": case "0x24": case "0x25": case "0x26": case "0x27": case "0x28": case "0x29": case "0x2A": case "0x2B": case "0x2C": case "0x2D": case "0x2E": case "0x2F":
            case "0x30": case "0x31": case "0x32": case "0x33": case "0x34": case "0x35": case "0x36": case "0x37": case "0x38": case "0x39": case "0x3A": case "0x3B": case "0x3C": case "0x3D": case "0x3E": case "0x3F":
            case "0x40": case "0x41": case "0x42": case "0x43": case "0x44": case "0x45": case "0x46": case "0x47": case "0x48": case "0x49": case "0x4A": case "0x4B": case "0x4C": case "0x4D": case "0x4E": case "0x4F":
            case "0x50": case "0x51": case "0x52": case "0x53": case "0x54": case "0x55": case "0x56": case "0x57": case "0x58": case "0x59": case "0x5A": case "0x5B": case "0x5C": case "0x5D": case "0x5E": case "0x5F":
            case "0x60": case "0x61": case "0x62": case "0x63": case "0x64": case "0x65": case "0x66": case "0x67": case "0x68": case "0x69": case "0x6A": case "0x6B": case "0x6C": case "0x6D": case "0x6E": case "0x6F":
            case "0x70": case "0x71": case "0x72": case "0x73": case "0x74": case "0x75": case "0x76": case "0x77": case "0x78": case "0x79": case "0x7A": case "0x7B": case "0x7C": case "0x7D": case "0x7E": case "0x7F":
                return "ITU-T Rec. H.222.0｜ISO/IEC 13818-1 reserved";
            default: // 0x80 ~ 0xFF : User private
                return "User private";
        }
    }
}
