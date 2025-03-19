// main.cpp

extern "C" {
#include <libavformat/avformat.h>
#include <libavutil/timestamp.h>
}

#include <iostream>
#include <unistd.h>

int main(int argc, char* argv[]) {
    if (argc < 2) {
        std::cerr << "Usage: " << argv[0] << " <video_file>" << std::endl;
        return 1;
    }

    const char* input_file = argv[1];
    AVPacket* packet = av_packet_alloc();
    if (!packet) {
        std::cerr << "FileCapturer::Start av_packet_alloc fail";
        return -1;
    }
    av_init_packet(packet);

    AVFormatContext* format_context = nullptr;
    int error =
        avformat_open_input(&format_context, input_file, nullptr, nullptr);
    if (error < 0) {
        std::cerr << "FileCapturer::Start avformat_open_input fail "
                        << input_file << " " << error;//av_err2str(error);
        av_packet_free(&packet);
        return -2;
    }

    error = avformat_find_stream_info(format_context, nullptr);
    if (error < 0) {
        std::cerr << "FileCapturer::Start avformat_find_stream_info fail "
                        << error;//av_err2str(error);
        av_packet_free(&packet);
        avformat_close_input(&format_context);
        return -3;
    }

    int v_stream_no = av_find_best_stream(format_context, AVMEDIA_TYPE_VIDEO, -1,
                                            -1, nullptr, 0);
    if (v_stream_no < 0 ||
        format_context->streams[v_stream_no]->time_base.den <= 0) {
        std::cerr << "FileCapturer::Start av_find_best_stream fail "
                        << v_stream_no;//av_err2str(v_stream_no);
        av_packet_free(&packet);
        avformat_close_input(&format_context);
        return -4;
    }
    AVStream* v_stream = format_context->streams[v_stream_no];

    bool first_packet = true;
    int64_t last_pts_ms = 0;
    int64_t emit_next_packet_ms = 0;

    bool need_loop_again = false;
    bool running_ = true;
    while (running_) {
        int ret = av_read_frame(format_context, packet);
        if (ret < 0 && ret != AVERROR_EOF) {
            std::cerr << "FileCapturer::Start av_read_frame fail "
                            << ret;//av_err2str(ret);
            running_ = false;
            break;
        }
        if (ret == AVERROR_EOF) {
            if (true) {
                std::cerr << "FileCapturer::Start got EOF, loop again";
                running_ = false;
                need_loop_again = true;
                break;
            } else {
                std::cerr << "FileCapturer::Start got EOF, quit";
                running_ = false;
                break;
            }
        }
        if (packet->stream_index != v_stream->index) {
            continue;
        }
        int64_t now_ms = 0;//rtc::TimeMicros() / 1000;
        int64_t this_pts_ms = 1000 * packet->pts * v_stream->time_base.num /
                                v_stream->time_base.den;
        if (first_packet) {
            emit_next_packet_ms = now_ms;
            last_pts_ms = this_pts_ms;
        }
        emit_next_packet_ms += this_pts_ms - last_pts_ms;
        last_pts_ms = this_pts_ms;
        int sleep_ms = (int)(emit_next_packet_ms - now_ms);

        // 获取流的时间基
        AVRational time_base = v_stream->time_base;

        // 将 pts 转换为毫秒
        int64_t pts_ms = av_rescale_q(packet->pts, time_base, (AVRational){1, 1000});
        first_packet = false;
        printf("av_read_frame packet->stream_index %d, v_stream->index %d, v_stream_no %d, packet->pts %d, v_stream->time_base.num %d, v_stream->time_base.den %d, this_pts_ms %d, pts_ms %d\n", packet->stream_index, v_stream->index, v_stream_no, (int) packet->pts, v_stream->time_base.num, v_stream->time_base.den, (int) this_pts_ms, (int) pts_ms);

        if (!first_packet && sleep_ms > 5) {
    #if defined(WEBRTC_WIN)
            Sleep(sleep_ms);
    #else
            usleep(sleep_ms * 1000);
    #endif
        }

        // if (video_callback_ && running_) {
        //     rtc::scoped_refptr<webrtc::TransitVideoFrameBuffer> frame_buffer =
        //         rtc::make_ref_counted<webrtc::TransitVideoFrameBuffer>(
        //             width_, height_, packet->size);
        //     memcpy(frame_buffer->mutable_data(), packet->data, packet->size);

        //     video_callback_->OnFrame(
        //         VideoFrame::Builder()
        //             .set_video_frame_buffer(frame_buffer)
        //             .set_rotation(VideoRotation::kVideoRotation_0)
        //             .set_dummy(false)
        //             .set_transit(true)
        //             .set_rtp_timestamp(0)
        //             .set_timestamp_ms(rtc::TimeMillis())
        //             .build());
        // }
    }

    av_packet_free(&packet);
    avformat_close_input(&format_context);

    return 0;
}
